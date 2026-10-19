#!/usr/bin/env python3
"""Upgrade legacy sample pairs with real audience demand (maintenance only).

Pass --source pointing to a directory of the original schema-1 definition and
both result files. Re-running requires that original directory, never an already
upgraded sample. BuildTimetableVariants reviews the saved complete assignments.
"""
import argparse
import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEST = ROOT / 'timetable-workspace/src/main/resources/demo/timetables'


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def room_name(definition, subject_name, room_id):
    prefix = {
        'Inglise keel': 'Keelteklass',
        'Flight Simulation': 'Flight Simulator Bay',
        'Starship Systems': 'Systems lab',
        'Starfighter Operations': 'Flight Simulator Bay',
        'Fleet Tactics': 'Tactical Holosuite',
        'Mission Planning': 'Mission lab',
    }[subject_name]
    numbers = [int(r['displayName'][len(prefix)+1:]) for r in definition['rooms']
               if r['id'] != room_id and r['displayName'].startswith(prefix+' ')
               and r['displayName'][len(prefix)+1:].isdigit()]
    return f'{prefix} {max(numbers, default=0)+1}'


def split(cohort, key, name, count, sizes=None):
    sizes = sizes or [cohort['size'] // count + (i < cohort['size'] % count) for i in range(count)]
    value = dict(id=f"{cohort['id']}-{key}", displayName=name, groupCount=count,
                 subjectIds=[], groups=[dict(id=f"{cohort['id']}-{key}-{i+1}", displayName=f'Group {i+1}', size=size)
                                       for i, size in enumerate(sizes)])
    cohort.setdefault('splits', []).append(value)
    return value


def course(d, variants, cohort, subject_name, arrangement, whole=0):
    subject = next(s for s in d['subjects'] if s['displayName'] == subject_name)['id']
    original = sorted([l for l in d['lessons'] if l['cohortId'] == cohort['id'] and l['subjectId'] == subject], key=lambda l: l['id'])
    assert original and whole < len(original)
    original_ids = {l['id'] for l in original}
    capabilities = sorted({capability for l in original for capability in l.get('requiredRoomCapabilityIds', [])})
    d['lessons'] = [l for l in d['lessons'] if l['id'] not in original_ids]
    arrangement['subjectIds'].append(subject)
    plan_id = f"{cohort['id']}-{subject}-teaching"
    plan = dict(id=plan_id, classId=cohort['id'], subjectId=subject, weeklyTotal=len(original),
                wholeClassCount=whole, delivery='mixed' if whole else 'grouped', splitId=arrangement['id'],
                overrides={}, portions=[])
    originals = [{a['lessonId']: a for a in rows if a['lessonId'] in original_ids} for rows in variants]
    for rows in variants:
        rows[:] = [a for a in rows if a['lessonId'] not in original_ids]
    audiences = ([dict(id='class:'+cohort['id'], displayName=cohort['displayName'], size=cohort['size'])] if whole else []) + arrangement['groups']
    for i, group in enumerate(audiences):
        is_whole = group['id'].startswith('class:')
        source = original[:whole] if is_whole else original[whole:]
        portion_id = f'{plan_id}-portion-{i+1}'
        # The original course teacher remains responsible for whole-class/first
        # group work. Additional instructors and small labs make parallel demand
        # real without overbooking the existing faculty or whole-class rooms.
        original_teacher = original[0]['teacherId']
        assert all(l['teacherId'] == original_teacher for l in original)
        if is_whole or i == (1 if whole else 0):
            teacher = next(t for t in d['teachers'] if t['id'] == original_teacher)
            room = None
        else:
            teacher = dict(id=f'{portion_id}-teacher', displayName=EXTRA_NAMES.pop(0), qualifiedSubjectIds=[subject],
                           availablePeriodIds=[p['id'] for p in d['periods']], eligibleClassIdsBySubject={})
            d['teachers'].append(teacher)
            room = dict(id=f'{portion_id}-room', displayName=room_name(d, subject_name, f'{portion_id}-room'),
                        capacity=group['size'], capabilityIds=original[0].get('requiredRoomCapabilityIds', []),
                        availablePeriodIds=[p['id'] for p in d['periods']])
            d['rooms'].append(room)
        if subject_name == 'Inglise keel':
            room = dict(id=f'{portion_id}-room', displayName=room_name(d, subject_name, f'{portion_id}-room'),
                        capacity=group['size'], capabilityIds=capabilities,
                        availablePeriodIds=[p['id'] for p in d['periods']])
            existing = next((r for r in d['rooms'] if r['id'] == room['id']), None)
            if existing:
                existing.update(room)
            else:
                d['rooms'].append(room)
        eligible = teacher['eligibleClassIdsBySubject'].setdefault(subject, [])
        if cohort['id'] not in eligible:
            eligible.append(cohort['id'])
        portion = dict(id=portion_id, audienceId=group['id'], sessionIds=[f"{l['id']}-audience-{i+1}" for l in source],
                       teacherPolicy=dict(mode='fixed', fixedTeacherId=teacher['id']))
        if room:
            portion['fixedRoomId'] = room['id']
        plan['portions'].append(portion)
        for rows, old in zip(variants, originals):
            for lesson, session in zip(source, portion['sessionIds']):
                assignment = copy.deepcopy(old[lesson['id']])
                assignment.update(lessonId=session, audienceId=group['id'], allocationId=portion_id, teacherId=teacher['id'])
                if room:
                    assignment['roomId'] = room['id']
                rows.append(assignment)
    plan['requiredRoomCapabilityIds'] = capabilities
    d['audiencePlans'].append(plan)
    d['coordinationRules'].append(dict(id=plan_id+'-parallel', classId=cohort['id'], type='START_TOGETHER', count=len(original)-whole,
                                     activities=[dict(portionId=p['id'], unit='SESSION') for p in plan['portions'] if not p['audienceId'].startswith('class:')]))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', required=True, type=Path)
    parser.add_argument('--school', choices=['full', 'star-wars'], action='append')
    args = parser.parse_args()
    for school in args.school or ['full', 'star-wars']:
        d = json.loads((args.source / f'{school}-definition.json').read_text())
        assert d['schemaVersion'] == 1, 'Supply the original schema-1 sample pairs'
        variants = [json.loads((args.source / f'{school}-{v}-result.json').read_text())['timetable']['assignments'] for v in ['a', 'b']]
        d.update(schemaVersion=2, catalogVersion=11, audiencePlans=[], coordinationRules=[])
        for t in d['teachers']:
            t['eligibleClassIdsBySubject'] = {}
        if school == 'full':
            EXTRA_NAMES[:] = ['Liina Metsalu', 'Kristi Pärnamaa', 'Siiri Kivimäe', 'Kairi Lehtmaa',
                             'Maris Ojaveer', 'Eveli Saarepuu', 'Anu Kaskla', 'Piret Vahermaa']
            subject = next(s['id'] for s in d['subjects'] if s['displayName'] == 'Inglise keel')
            english_classes = {l['cohortId'] for l in d['lessons'] if l['subjectId'] == subject}
            for c in d['cohorts']:
                if c['id'] not in english_classes:
                    continue
                english = split(c, 'english', 'Inglise keele rühmad', 2)
                course(d, variants, c, 'Inglise keel', english)
        else:
            EXTRA_NAMES[:] = ['Maarek Stele', 'Dara Ors', 'Ved Foslo', 'Nash Windrider', 'Seyn Marana', 'ISB Training Droid KX-7', 'Command Instructor Rho', 'Command Instructor Sigma']
            for day, weekday in enumerate(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'], 1):
                for order, start, end in [(8, '16:15:00', '17:05:00'), (9, '17:15:00', '18:05:00'), (10, '18:15:00', '19:05:00')]:
                    d['periods'].append(dict(id=f'imperial-period-{day}-{order}', displayName=f'{weekday.title()} {order}', weekday=weekday, order=order, startTime=start, endTime=end))
            d['periods'].sort(key=lambda p: (['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'].index(p['weekday']),p['order']))
            for resource in d['teachers'] + d['rooms']:
                resource['availablePeriodIds'] = [p['id'] for p in d['periods']]
            c = next(c for c in d['cohorts'] if c['id'].endswith('-4A'))
            crews = split(c, 'flight', 'Simulator wings', 2, [13, 12])
            labs = split(c, 'engineering', 'Engineering crews', 4, [7,6,6,6])
            c['relationships'] = dict(mode='nested', splitIds=[crews['id'], labs['id']], constituentSplitId=labs['id'],
                                      unions={crews['groups'][0]['id']:[g['id'] for g in labs['groups'][:2]], crews['groups'][1]['id']:[g['id'] for g in labs['groups'][2:]]}, intersections=[])
            course(d, variants, c, 'Flight Simulation', crews)
            course(d, variants, c, 'Starship Systems', labs)
            c = next(c for c in d['cohorts'] if c['id'].endswith('-7A'))
            flight = split(c, 'flight', 'Starfighter squadrons', 2, [13,13])
            tactics = split(c, 'tactics', 'Tactical command teams', 2, [13,13])
            c['relationships'] = dict(mode='joint', splitIds=[flight['id'], tactics['id']], unions={}, intersections=[dict(groupIds=[flight['groups'][i]['id'],tactics['groups'][j]['id']],count=count) for i,j,count in [(0,0,7),(0,1,6),(1,0,6),(1,1,7)]])
            course(d, variants, c, 'Starfighter Operations', flight)
            course(d, variants, c, 'Fleet Tactics', tactics)
            c = next(c for c in d['cohorts'] if c['id'].endswith('-8A'))
            commands = split(c, 'command', 'Mission planning cells', 3, [10,9,9])
            course(d, variants, c, 'Mission Planning', commands, whole=1)
        for p in d['periods']:
            following = next((q for q in d['periods'] if q['weekday'] == p['weekday'] and q['order'] == p['order']+1), None)
            # A long lunch break divides student-gap accounting and blocks.
            def minutes(value):
                h, m, *_ = value.split(':')
                return int(h)*60+int(m)
            lunch = following is not None and minutes(following['startTime'])-minutes(p['endTime']) >= 30
            # Kasevälja's legacy catalog declares no consecutive-block grid;
            # retain that declaration when upgrading its audience contract.
            p['adjacentToNext'] = school == 'star-wars' and following is not None and not lunch
            p['gapBoundaryAfter'] = lunch
        write(DEST / f'{school}-definition.json', d)
        for v, rows in zip(['a','b'], variants):
            write(ROOT / f'tools/demo/seeds/{school}-{v}-assignments.json', rows)
        print(school, 'audience plans:',len(d['audiencePlans']), 'actual sessions:',len(variants[0]), 'periods:',len(d['periods']))


EXTRA_NAMES = []
if __name__ == '__main__':
    main()
