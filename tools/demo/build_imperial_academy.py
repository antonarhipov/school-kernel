#!/usr/bin/env python3
"""Independently construct the Imperial academy's progressive curriculum and feasible seed.

Maintenance only. BuildTimetableVariants and record_timetable_evidence must evaluate
and verify the complete results; ordinary builds never run this generator.
"""
import collections
import json
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEST = ROOT / 'timetable-workspace/src/main/resources/demo/timetables'
# Subject -> (public name, qualified faculty group, required specialist facility).
SUBJECTS = {
    'basic': ('Galactic Basic', 'communications', None),
    'numeracy': ('Applied Numeracy', 'science', None),
    'history': ('Galactic History', 'civics', None),
    'natural-science': ('Natural Sciences', 'science', None),
    'droid-literacy': ('Droid Literacy', 'technology', 'workshop'),
    'conditioning': ('Physical Conditioning', 'fitness', 'conditioning'),
    'citizenship': ('Academy Citizenship', 'civics', None),
    'teamwork': ('Teamwork and First Aid', 'medicine', 'medical'),
    'communication': ('Technical Communication', 'communications', None),
    'physics': ('Applied Physics', 'science', 'physics'),
    'navigation': ('Astrogation Fundamentals', 'navigation', 'navigation'),
    'systems': ('Starship Systems', 'technology', 'workshop'),
    'simulation': ('Flight Simulation', 'flight', 'flight'),
    'tactical-basics': ('Tactical Fundamentals', 'tactics', 'tactical'),
    'survival': ('Fieldcraft and Survival', 'fieldcraft', 'field'),
    'emergency-medicine': ('Emergency Medicine', 'medicine', 'medical'),
    'command-law': ('Command Ethics and Law', 'civics', None),
    'advanced-navigation': ('Advanced Astrogation', 'navigation', 'navigation'),
    'flight-operations': ('Starfighter Operations', 'flight', 'flight'),
    'fleet-tactics': ('Fleet Tactics', 'tactics', 'tactical'),
    'combined-operations': ('Combined Operations', 'tactics', 'field'),
    'mission-planning': ('Mission Planning', 'intelligence', 'intelligence'),
    'intelligence': ('Intelligence Analysis', 'intelligence', 'intelligence'),
    'engineering': ('Starship Engineering', 'technology', 'workshop'),
    'leadership': ('Field Leadership', 'fieldcraft', 'field'),
    'combat-medicine': ('Combat Medicine', 'medicine', 'medical'),
    'briefing': ('Officer Briefing', 'communications', None),
}
CURRICULA = {
    'junior': dict(basic=5, numeracy=5, history=3, **{'natural-science': 3, 'droid-literacy': 2},
                   conditioning=3, citizenship=2, teamwork=2),
    'intermediate': dict(communication=3, physics=4, navigation=3, systems=3, simulation=3,
                         **{'tactical-basics': 3, 'survival': 3, 'conditioning': 3,
                            'emergency-medicine': 2, 'command-law': 3}),
    'senior': {'advanced-navigation': 3, 'flight-operations': 4, 'fleet-tactics': 4,
               'combined-operations': 4, 'mission-planning': 4, 'intelligence': 3,
               'engineering': 3, 'leadership': 3, 'combat-medicine': 2,
               'conditioning': 3, 'briefing': 2},
}
FACULTY = {
    'communications': ['C-3PO', 'AP-5', 'Eli Vanto'],
    'science': ['Galen Erso', 'Bevel Lemelisk', 'Qwi Xux'],
    'civics': ['Wilhuff Tarkin', 'Mas Amedda'],
    'technology': ['Orson Krennic', 'Del Meeko', 'Bodhi Rook'],
    'fitness': ['Captain Phasma', 'FN-2199', 'Sergeant Kreel'],
    'medicine': ['AZI-3', '2-1B'],
    'navigation': ['Firmus Piett', 'Gilad Pellaeon'],
    'flight': ['Soontir Fel', 'Ciena Ree', 'Vult Skerris'],
    'tactics': ['Grand Admiral Thrawn', 'Maximilian Veers', 'Iden Versio'],
    'fieldcraft': ['Alexsandr Kallus', 'Gideon Hask'],
    'intelligence': ['Wullf Yularen', 'Dedra Meero', 'Rae Sloane'],
}
FACILITIES = {'workshop': ('Droid and Starship Workshop', 2),
              'conditioning': ('Conditioning Hall', 2), 'medical': ('Medical Training Bay', 2),
              'physics': ('Physics Laboratory', 1), 'navigation': ('Astrogation Laboratory', 2),
              'flight': ('Flight Simulator Bay', 3), 'tactical': ('Tactical Holosuite', 3),
              'field': ('Field Training Arena', 2), 'intelligence': ('Intelligence Suite', 2)}
STAGES = {'junior': ('Junior Cadet', range(1, 4), 5),
          'intermediate': ('Intermediate Cadet', range(4, 7), 6),
          'senior': ('Senior Officer Cadet', range(7, 10), 7)}


def ident(kind, key):
    return f'imperial-{kind}-{key}'


def construct(classes, teaching):
    # Each stage has the same length of day throughout the week. Resource and
    # fixed course-teacher occupancy participate in construction, never just labels.
    for attempt in range(150):
        rng = random.Random(20261018 + attempt)
        occupied = collections.Counter()
        busy = collections.defaultdict(set)
        placements = {}
        for stage, level, form in sorted(classes, key=lambda c: (-STAGES[c[0]][2], rng.random())):
            slots = [(day, slot) for day in range(5) for slot in range(STAGES[stage][2])]
            counts = CURRICULA[stage]
            for trial in range(2000):
                available, daily, assigned = set(slots), collections.Counter(), []
                subjects = sorted(counts, key=lambda s: (-counts[s], rng.random()))
                for subject in subjects:
                    teacher = teaching[stage, level, form, subject]
                    facility = SUBJECTS[subject][2]
                    for _ in range(counts[subject]):
                        choices = [(day, slot) for day, slot in sorted(available)
                                   if not daily[day, subject] and teacher not in busy[day, slot]
                                   and (not facility or occupied[day, slot, facility] < FACILITIES[facility][1])]
                        if not choices:
                            break
                        day, slot = rng.choice(choices)
                        available.remove((day, slot)); daily[day, subject] += 1
                        assigned.append(((day, slot), subject))
                    else:
                        continue
                    break
                else:
                    placements[stage, level, form] = sorted(assigned)
                    for (day, slot), subject in assigned:
                        busy[day, slot].add(teaching[stage, level, form, subject])
                        if facility := SUBJECTS[subject][2]: occupied[day, slot, facility] += 1
                    break
            else:
                break
        else:
            return placements
    raise RuntimeError('Could not construct balanced academy seed within faculty/facility limits')


def main():
    classes = [(stage, level, form) for stage, (_, levels, _) in STAGES.items()
               for level in levels for form in 'AB']
    teaching, loads = {}, collections.Counter()
    for stage, level, form in classes:
        for subject, count in CURRICULA[stage].items():
            group = SUBJECTS[subject][1]
            teacher = min((ident('teacher', f'{group}-{n+1}') for n in range(len(FACULTY[group]))),
                          key=lambda t: (loads[t], t))
            teaching[stage, level, form, subject] = teacher; loads[teacher] += count
    placements = construct(classes, teaching)
    periods = [dict(id=ident('period', f'{day+1}-{slot+1}'), displayName=f'{weekday.title()} {slot+1}',
                    weekday=weekday, order=slot+1, startTime=start+':00', endTime=end+':00')
               for day, weekday in enumerate(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'])
               for slot, (start, end) in enumerate([('08:30', '09:20'), ('09:30', '10:20'),
                   ('10:40', '11:30'), ('11:40', '12:30'), ('13:15', '14:05'),
                   ('14:15', '15:05'), ('15:15', '16:05')])]
    all_periods = [p['id'] for p in periods]
    teachers = [dict(id=ident('teacher', f'{group}-{n+1}'), displayName=name,
                     qualifiedSubjectIds=[ident('subject', s) for s, (_, g, _) in SUBJECTS.items() if g == group],
                     availablePeriodIds=all_periods)
                for group, people in FACULTY.items() for n, name in enumerate(people)]
    rooms = [dict(id=ident('room', f'{level}{form}'), displayName=f'Cadet Classroom {100+level*10+ord(form)-64}',
                  capacity=30, capabilityIds=[ident('capability', 'classroom')], availablePeriodIds=all_periods)
             for _, level, form in classes]
    facilities = {}
    for key, (name, count) in FACILITIES.items():
        facilities[key] = []
        for n in range(1, count+1):
            room = dict(id=ident('room', f'{key}-{n}'), displayName=f'{name} {n}', capacity=32,
                        capabilityIds=[ident('capability', key)], availablePeriodIds=all_periods)
            rooms.append(room); facilities[key].append(room['id'])
    cohorts, lessons, seed = [], [], []
    busy_rooms = collections.defaultdict(set)
    for index, (stage, level, form) in enumerate(classes):
        cohort_id, home = ident('cohort', f'{stage}-{level}{form}'), ident('room', f'{level}{form}')
        name = f'{STAGES[stage][0]} {level}{form}'
        curator_subject = 'citizenship' if stage == 'junior' else 'command-law' if stage == 'intermediate' else 'leadership'
        cohorts.append(dict(id=cohort_id, displayName=name, size=24+index % 5, homeRoomId=home,
                            curatorTeacherId=teaching[stage, level, form, curator_subject],
                            latestStartSlot=1, maxDailyGaps=0, dailyLessonSpreadLimit=0))
        occurrences = collections.Counter()
        for (day, slot), subject in placements[stage, level, form]:
            facility = SUBJECTS[subject][2]
            candidates = facilities[facility] if facility else [home]
            room = next(r for r in candidates if r not in busy_rooms[day, slot])
            busy_rooms[day, slot].add(room); occurrences[subject] += 1
            lesson_id = ident('lesson', f'{stage}-{level}{form}-{subject}-{occurrences[subject]}')
            lesson = dict(id=lesson_id, displayName=f'{name} · {SUBJECTS[subject][0]}',
                          subjectId=ident('subject', subject), cohortId=cohort_id,
                          teacherId=teaching[stage, level, form, subject],
                          seriesId=ident('series', f'{stage}-{level}{form}-{subject}'),
                          requiredRoomCapabilityIds=[ident('capability', facility or 'classroom')])
            if not facility: lesson['preferredRoomIds'] = [home]
            lessons.append(lesson)
            seed.append(dict(lessonId=lesson_id, periodId=ident('period', f'{day+1}-{slot+1}'), roomId=room))
    assert len(loads) == len(teachers) and max(loads.values()) <= 26
    definition = dict(schemaVersion=1, catalogVersion=9, schoolId='demo-star-wars-school',
                      displayName='Coruscant Imperial Academy',
                      subjects=[dict(id=ident('subject', s), displayName=info[0], maxDailyLessonsPerCohort=1)
                                for s, info in SUBJECTS.items()],
                      teachers=teachers, rooms=rooms, cohorts=cohorts, periods=periods, lessons=lessons)
    for path, value in [(DEST/'star-wars-definition.json', definition),
                        (ROOT/'tools/demo/seeds/star-wars-assignments.json', seed)]:
        path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n')
    print('Coruscant Imperial Academy:', len(cohorts), 'cohorts;', len(lessons), 'weekly lessons;',
          len(teachers), 'teachers;', len(rooms), 'rooms; teacher loads', min(loads.values()), 'to', max(loads.values()))


if __name__ == '__main__':
    main()
