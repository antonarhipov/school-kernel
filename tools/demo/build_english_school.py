#!/usr/bin/env python3
"""Construct Meadowbrook independently of the source-derived schools (maintenance only).

Produces a definition and a collision-free seed. BuildTimetableVariants and the real
kernel must evaluate/verify both results before record_timetable_evidence may ship them.
No source example or existing school placement is read.
"""
import collections
import json
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEST = ROOT / 'timetable-workspace/src/main/resources/demo/timetables'
SUBJECTS = {
    'english': 'English', 'maths': 'Mathematics', 'science': 'Science',
    'history': 'History', 'geography': 'Geography', 'french': 'French',
    'spanish': 'Spanish', 'pe': 'Physical Education', 'art': 'Art and Design',
    'music': 'Music', 'computing': 'Computing', 'technology': 'Design and Technology',
    'pshe': 'Personal, Social and Health Education',
}
GROUP = {'history': 'humanities', 'geography': 'humanities', 'pshe': 'humanities',
         'french': 'languages', 'spanish': 'languages'}
STAFF = {'english': 4, 'maths': 4, 'science': 5, 'humanities': 4, 'languages': 4,
         'pe': 4, 'art': 2, 'music': 2, 'computing': 2, 'technology': 2}
SPECIAL = {'science': ('Science Laboratory', 3), 'pe': ('Sports Hall', 2),
           'art': ('Art Studio', 2), 'music': ('Music Room', 2),
           'computing': ('Computer Suite', 2), 'technology': ('Design Workshop', 2)}
PRIMARY = {'english', 'maths', 'science', 'history', 'geography', 'pshe'}
NAMES = [
    'Alice Whitcombe', 'Oliver Fenwick', 'Amelia Hartwell', 'George Ashcroft',
    'Isla Brookfield', 'Harry Westcott', 'Grace Ellerton', 'Jack Wrenford',
    'Sophie Alderwick', 'Thomas Fairbourne', 'Freya Henshaw', 'William Oakley',
    'Lily Bracken', 'James Merrivale', 'Florence Wycliffe', 'Henry Calder',
    'Evie Northwood', 'Arthur Bellamy', 'Charlotte Winslow', 'Oscar Redfern',
    'Matilda Fallow', 'Leo Penrose', 'Emily Thorne', 'Archie Ashwell',
    'Poppy Langford', 'Samuel Briar', 'Imogen Marchwood', 'Daniel Ridley',
    'Rosie Whitford', 'Edward Beckett', 'Lucy Hartley', 'Joseph Waverley',
    'Eleanor Foxley', 'Benjamin Cresswell', 'Daisy Rowan', 'Frederick Dale',
    'Hannah Elmhurst', 'Alexander Hawthorne', 'Clara Woodleigh', 'Nathan Everley',
    'Martha Greenwell',
]


def ident(kind, key):
    return f'meadowbrook-{kind}-{key}'


def curriculum(year, form):
    if year <= 4:
        return dict(english=7, maths=5, science=2, history=1, geography=1,
                    pe=2, art=2, music=1, computing=1, technology=1, pshe=2)
    counts = dict(english=6 if year == 5 else 5, maths=5,
                  science=3 if year == 5 else 5, history=2, geography=2,
                  pe=2, art=1, music=1, computing=1,
                  technology=1 if year == 5 else 2, pshe=1)
    counts['spanish' if form == 'B' else 'french'] = 2 if year == 5 else 3
    return counts


def resource(year, subject):
    # Lower-school science is taught in the class's home room.
    if year <= 4 and subject in PRIMARY:
        return None
    return GROUP.get(subject, subject)


def construct():
    classes = [(year, form) for year in range(1, 10)
               for form in ('AB' if year <= 5 else 'ABC')]
    teaching, loads = {}, collections.Counter()
    for index, (year, form) in enumerate(classes):
        for subject, count in curriculum(year, form).items():
            group = resource(year, subject)
            candidates = [ident('teacher', f'primary-{index+1}')] if group is None else [
                ident('teacher', f'{group}-{n+1}') for n in range(STAFF[group])]
            teacher = min(candidates, key=lambda t: (loads[t], t))
            teaching[year, form, subject] = teacher
            loads[teacher] += count
    capacities = {group: min(count, SPECIAL[group][1]) if group in SPECIAL else count
                  for group, count in STAFF.items()}
    # Construct complete per-class curricula, distributing each subject across days
    # while respecting global specialist staffing/room concurrency at each period.
    for attempt in range(200):
        rng = random.Random(20261017 + attempt)
        occupancy = collections.Counter()
        busy = collections.defaultdict(set)
        placements = {}
        order = sorted(classes, key=lambda c: (-sum(curriculum(*c).values()), rng.random()))
        for year, form in order:
            counts = curriculum(year, form)
            slots = [(day, slot) for day in range(5)
                     for slot in range(6 if year >= 6 or year == 5 and day in (1, 3) else 5)]
            for trial in range(1500):
                available = set(slots)
                assigned = []
                daily = collections.Counter()
                subjects = sorted(counts, key=lambda s: (-counts[s], rng.random()))
                for subject in subjects:
                    group = resource(year, subject)
                    teacher = teaching[year, form, subject]
                    for occurrence in range(counts[subject]):
                        choices = [(day, slot) for day, slot in sorted(available)
                                   if daily[day, subject] < (2 if counts[subject] > 5 else 1)
                                   and teacher not in busy[day, slot]
                                   and (not group or occupancy[day, slot, group] < capacities[group])]
                        if not choices:
                            break
                        day, slot = rng.choice(choices)
                        available.remove((day, slot)); daily[day, subject] += 1
                        assigned.append(((day, slot), subject))
                    else:
                        continue
                    if len(assigned) < len(slots):
                        break
                else:
                    placements[year, form] = sorted(assigned)
                    for (day, slot), subject in placements[year, form]:
                        if group := resource(year, subject): occupancy[day, slot, group] += 1
                        busy[day, slot].add(teaching[year, form, subject])
                    break
            else:
                break
        else:
            return classes, placements, teaching
    raise RuntimeError('Could not construct independent weekly curricula within resource limits')


def main():
    classes, placements, teaching = construct()
    periods = [dict(id=ident('period', f'{day+1}-{slot+1}'),
                    displayName=f'{weekday.title()} {slot+1}', weekday=weekday, order=slot+1,
                    startTime=f'{hour:02}:{minute:02}:00', endTime=f'{end//60:02}:{end%60:02}:00')
               for day, weekday in enumerate(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'])
               for slot, (hour, minute, end) in enumerate([(8, 45, 575), (9, 40, 630),
                   (10, 50, 700), (11, 45, 755), (13, 20, 850), (14, 15, 905)])]
    all_periods = [p['id'] for p in periods]
    teachers, pools = [], {}
    for group, count in [('primary', 8), *STAFF.items()]:
        subjects = PRIMARY if group == 'primary' else [s for s in SUBJECTS if GROUP.get(s, s) == group]
        pools[group] = []
        for n in range(count):
            teacher = dict(id=ident('teacher', f'{group}-{n+1}'), displayName=NAMES[len(teachers)],
                           qualifiedSubjectIds=[ident('subject', s) for s in sorted(subjects)],
                           availablePeriodIds=all_periods)
            teachers.append(teacher); pools[group].append(teacher['id'])
    rooms = [dict(id=ident('room', f'{year}{form}'), displayName=f'Classroom {100+year*10+ord(form)-64}',
                  capacity=30, capabilityIds=[ident('capability', 'classroom')], availablePeriodIds=all_periods)
             for year, form in classes]
    room_pools = {}
    for subject, (name, count) in SPECIAL.items():
        room_pools[subject] = []
        for n in range(1, count+1):
            room = dict(id=ident('room', f'{subject}-{n}'), displayName=f'{name} {n}',
                        capacity=32, capabilityIds=[ident('capability', subject)], availablePeriodIds=all_periods)
            rooms.append(room); room_pools[subject].append(room['id'])
    cohorts, lessons, seed = [], [], []
    busy_teachers, busy_rooms, workloads = collections.defaultdict(set), collections.defaultdict(set), collections.Counter()
    for class_index, (year, form) in enumerate(classes):
        cohort_id, home = ident('cohort', f'{year}{form}'), ident('room', f'{year}{form}')
        primary = pools['primary'][class_index] if year <= 4 else None
        curator = primary or pools['humanities'][(class_index-8) % 4]
        cohorts.append(dict(id=cohort_id, displayName=f'Year {year}{form}', size=24+(class_index % 5),
                            homeRoomId=home, curatorTeacherId=curator, latestStartSlot=1,
                            dailyLessonSpreadLimit=1))
        occurrences = collections.Counter()
        for (day, slot), subject in placements[year, form]:
            key = day, slot
            group = resource(year, subject)
            teacher = teaching[year, form, subject]
            assert teacher not in busy_teachers[key]
            room_candidates = room_pools[subject] if subject in SPECIAL and group is not None else [home]
            room = next(r for r in room_candidates if r not in busy_rooms[key])
            occurrences[subject] += 1
            lesson_id = ident('lesson', f'{year}{form}-{subject}-{occurrences[subject]}')
            lessons.append(dict(id=lesson_id, displayName=f'Year {year}{form} · {SUBJECTS[subject]}',
                                subjectId=ident('subject', subject), cohortId=cohort_id, teacherId=teacher,
                                seriesId=ident('series', f'{year}{form}-{subject}'),
                                requiredRoomCapabilityIds=[ident('capability', subject if subject in SPECIAL and group is not None else 'classroom')]))
            seed.append(dict(lessonId=lesson_id, periodId=ident('period', f'{day+1}-{slot+1}'), roomId=room))
            busy_teachers[key].add(teacher); busy_rooms[key].add(room); workloads[teacher] += 1
    assert len(workloads) == len(teachers), 'Every fictional staff member must teach'
    definition = dict(schemaVersion=1, catalogVersion=9, schoolId='demo-meadowbrook-school',
                      displayName='Meadowbrook Community School',
                      subjects=[dict(id=ident('subject', s), displayName=name,
                                     maxDailyLessonsPerCohort=2 if s == 'english' else 1)
                                for s, name in SUBJECTS.items()],
                      teachers=teachers, cohorts=cohorts, rooms=rooms, periods=periods, lessons=lessons)
    DEST.mkdir(parents=True, exist_ok=True)
    for path, value in [(DEST/'english-definition.json', definition), (ROOT/'tools/demo/seeds/english-assignments.json', seed)]:
        path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n')
    print('Meadowbrook:', ', '.join(f'{len(definition[k])} {k}' for k in ['cohorts', 'lessons', 'teachers', 'rooms', 'periods']))


if __name__ == '__main__':
    main()
