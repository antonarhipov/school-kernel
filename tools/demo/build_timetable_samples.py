#!/usr/bin/env python3
"""Build fictional demo curricula. Original staff identities are never emitted.

Run explicitly from the repository root, then BuildTimetableVariants.java and record_timetable_evidence.py.
The normal build consumes only the generated fictional resources, never the source examples.
"""
import copy
import json
import tempfile
from pathlib import Path

from generate_samples import generate
from build_english_school import main as build_english_school
from build_imperial_academy import main as build_imperial_academy

ROOT = Path(__file__).resolve().parents[2]
DEST = ROOT / 'timetable-workspace/src/main/resources/demo/timetables'
FIRST = ['Mari', 'Jaan', 'Katrin', 'Toomas', 'Liis', 'Martin', 'Kärt', 'Andres', 'Evelin', 'Siim',
         'Helen', 'Priit', 'Kadri', 'Rasmus', 'Annika', 'Tanel', 'Triin']
LAST = ['Tammela', 'Kaseväli', 'Metsaru', 'Laaneots', 'Põldmaa', 'Järveleht', 'Kivisalu']

def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def school(identifier, source_name, title, directory):
    original = json.loads((ROOT / f'examples/{source_name}.json').read_text())
    generate(identifier, source_name, title, directory)
    doc = json.loads((directory / f'{identifier}-definition.json').read_text())
    doc['schoolId'] = f'demo-{identifier}-school'
    # Restore public, non-personal curriculum/class labels by the original order-preserving map.
    for collection in ['subjects', 'cohorts']:
        names = {f'demo-{collection[:-1] if collection != "cohorts" else "cohort"}-{n:03}': item['displayName']
                 for n, item in enumerate(sorted(original[collection], key=lambda item: item['id']), 1)}
        for item in doc[collection]:
            item['displayName'] = names[item['id']]
    if source_name == 'mv5':
        for subject in doc['subjects']:
            if subject['displayName'] == 'IntÕ': subject['displayName'] = 'Klassijuhatajatund'
            if subject['displayName'].startswith('Täiendav'): subject['displayName'] = 'Õpiabi'
        support = next(s['id'] for s in doc['subjects'] if s['displayName'] == 'Õpiabi')
        doc['lessons'] = [lesson for lesson in doc['lessons'] if lesson['subjectId'] != support]
    people = [f'{first} {last}' for last in LAST for first in FIRST]
    real_people = {p['displayName'] for p in original['teachers']}
    people = [p for p in people if p not in real_people]
    for n, teacher in enumerate(doc['teachers']): teacher['displayName'] = people[n]
    if source_name == 'mvk':
        # Source primary-school blocks represent a class teacher's whole curriculum.
        # Expand them into actual subjects while retaining the proven lesson identities/placements.
        subjects_by_name = {s['displayName']: s['id'] for s in doc['subjects']}
        teachers = {t['id']: t for t in doc['teachers']}
        for cohort in doc['cohorts']:
            if not cohort['displayName'].startswith(('1', '2', '3')): continue
            blocks = [l for l in doc['lessons'] if l['cohortId'] == cohort['id']
                      and l['subjectId'] == subjects_by_name['Klassitund']]
            curriculum = ['Eesti keel'] * 6 + ['Matemaatika'] * 4 + ['Loodusõpetus'] * 2 \
                + ['Kunstiõpetus'] * 2 + ['Inimeseõpetus', 'Klassitund']
            curriculum += ['Matemaatika', 'Loodusõpetus']
            for lesson, name in zip(blocks, curriculum):
                lesson['subjectId'] = subjects_by_name[name]
                lesson['seriesId'] = cohort['id'] + '.' + lesson['subjectId']
                qualified = teachers[lesson['teacherId']]['qualifiedSubjectIds']
                if lesson['subjectId'] not in qualified: qualified.append(lesson['subjectId'])
        for subject in doc['subjects']:
            if subject['displayName'] == 'IntÕ': subject['displayName'] = 'Lõimitud õpe'
    for n, room in enumerate(doc['rooms']):
        room['displayName'] = f'Klassiruum {101+n}'
        capabilities = room.get('capabilityIds', [])
        # Synthetic capability IDs retain their proven original one-to-one mapping.
        source_room = original['rooms'][n]
        special = set(source_room['capabilityIds']) - {'general-classroom'}
        if special:
            names = {'sports-hall': 'Spordisaal', 'science-lab': 'Loodusteaduste labor', 'music-room': 'Muusikaklass',
                     'art-studio': 'Kunstiklass', 'workshop': 'Tööõpetuse klass', 'kitchen': 'Õppeköök', 'swimming-pool': 'Ujula'}
            room['displayName'] = f'{names.get(sorted(special)[0], "Eriklass")} {101+n}'
    if source_name == 'mv5':
        all_periods = [p['id'] for p in doc['periods']]
        additions = [('english', 'Inglise keel', 3, None), ('russian', 'Vene keel', 1, None),
                     ('pe', 'Kehaline kasvatus', 2, 'sports-hall'),
                     ('technology', 'Käsitöö ja tehnoloogiaõpetus', 2, 'workshop')]
        generic = doc['lessons'][0]['requiredRoomCapabilityIds']
        for index, (key, name, count, capability) in enumerate(additions):
            subject_id, teacher_id = f'demo-subject-{key}', f'demo-teacher-extra-{key}'
            doc['subjects'].append({'id': subject_id, 'displayName': name})
            doc['teachers'].append({'id': teacher_id, 'displayName': people[len(doc['teachers'])],
                                    'qualifiedSubjectIds': [subject_id], 'availablePeriodIds': all_periods})
            required = [f'demo-capability-{key}'] if capability else generic
            if capability:
                doc['rooms'].append({'id': f'demo-room-{key}', 'displayName': 'Spordisaal' if key == 'pe' else 'Tehnoloogiaklass',
                    'capacity': 30, 'capabilityIds': required, 'availablePeriodIds': all_periods})
            for cohort in doc['cohorts']:
                for occurrence in range(1, count+1):
                    doc['lessons'].append({'id': f'{cohort["id"]}.{key}.{occurrence}', 'subjectId': subject_id,
                        'cohortId': cohort['id'], 'teacherId': teacher_id, 'seriesId': f'{cohort["id"]}.{key}',
                        'requiredRoomCapabilityIds': required})
        # A real qualified alternative makes inspector teacher changes demonstrable without editing the catalog.
        english = copy.deepcopy(doc['teachers'][-4]); english['id'] = 'demo-teacher-english-cover'
        english['displayName'] = people[len(doc['teachers'])]; doc['teachers'].append(english)
    subjects = {s['id']: s['displayName'] for s in doc['subjects']}
    classes = {c['id']: c['displayName'] for c in doc['cohorts']}
    for lesson in doc['lessons']:
        lesson['displayName'] = f'{classes[lesson["cohortId"]]} · {subjects[lesson["subjectId"]]}'
    return doc


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='fictional-curricula-') as temporary:
        directory = Path(temporary)
        two = school('two-class', 'mv5', 'Ranniku õppehoone', directory)
        full = school('full', 'mvk', 'Kasevälja Põhikool', directory)
        for identifier, doc in [('two-class', two), ('full', full)]:
            write(DEST / f'{identifier}-definition.json', doc)
            print(identifier, len(doc['cohorts']), 'classes;', len(doc['lessons']), 'weekly lessons')
        # The already-feasible synthetic full-school placements are retained as a maintenance seed only.
        # Assignment seeds live outside shipped resources. BuildTimetableVariants reads them there.
    build_imperial_academy()
    build_english_school()


if __name__ == '__main__': main()
