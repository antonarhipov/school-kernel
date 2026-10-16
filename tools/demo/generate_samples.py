#!/usr/bin/env python3
"""Explicit maintenance command; ordinary builds never read the real-school examples.

Only fictionalized documents and nonidentifying generation evidence are emitted. Source maps stay in this process.
Regenerate result files separately with the current packaged kernel, then verify before committing.
"""
import copy
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DESTINATION = ROOT / "timetable-workspace/src/test/resources/demo/legacy-samples"
NAMES = json.loads(Path(__file__).with_name("names.json").read_text())
SAMPLES = [("small", "initial-school", "Small school"),
           ("two-class", "mv5", "Two-class school"), ("full", "mvk", "Full school")]
COLLECTIONS = {"subjects": "subject", "teachers": "teacher", "cohorts": "cohort", "rooms": "room",
               "periods": "period", "lessons": "lesson", "roomAssignments": "policy"}
FIELDS = {
    "schoolId": "school", "subjectId": "subject", "qualifiedSubjectIds": "subject",
    "teacherId": "teacher", "curatorTeacherId": "teacher", "cohortId": "cohort",
    "roomId": "room", "homeRoomId": "room", "roomLock": "room", "preferredRoomIds": "room",
    "allowedRoomIds": "room", "periodId": "period", "periodLock": "period",
    "availablePeriodIds": "period", "undesirablePeriodIds": "period", "reservedPeriodIds": "period",
    "lessonId": "lesson", "seriesId": "series", "capabilityIds": "capability",
    "requiredRoomCapabilityIds": "capability",
}


def canonical(document):
    return json.dumps(document, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()


def strings(document, field):
    values = []
    if isinstance(document, dict):
        for key, value in document.items():
            if key == field:
                values.extend(value if isinstance(value, list) else [value])
            values.extend(strings(value, field))
    elif isinstance(document, list):
        for value in document:
            values.extend(strings(value, field))
    return [value for value in values if isinstance(value, str)]


def generate(identifier, filename, title, destination=DESTINATION):
    source = json.loads((ROOT / f"examples/{filename}.json").read_text())
    identifiers = {kind: set() for kind in set(COLLECTIONS.values()) | set(FIELDS.values())}
    for collection, kind in COLLECTIONS.items():
        identifiers[kind].update(item["id"] for item in source.get(collection, []))
    for field, kind in FIELDS.items():
        identifiers[kind].update(strings(source, field))
    mappings = {kind: {old: f"demo-{kind}-{number:03}" for number, old in enumerate(sorted(values), 1)}
                for kind, values in identifiers.items()}
    display_reverse = {}

    def transform(value, kind=None, reverse=False):
        maps = {key: {v: k for k, v in entries.items()} for key, entries in mappings.items()} if reverse else mappings
        if isinstance(value, list):
            return [transform(item, kind, reverse) for item in value]
        if isinstance(value, str):
            return maps[kind][value] if kind is not None else value
        if not isinstance(value, dict):
            return value
        result = {}
        for key, item in value.items():
            target = COLLECTIONS.get(key) or FIELDS.get(key)
            if key == "id":
                target = kind
            if key == "displayName":
                if reverse:
                    result[key] = display_reverse[item]
                else:
                    if kind == "teacher":
                        index = sorted(identifiers[kind]).index(value["id"])
                        replacement = NAMES[index]
                    elif kind == "period":
                        replacement = f"{value['weekday'].title()} {value['order']}"
                    elif kind is None:
                        replacement = title
                    else:
                        index = sorted(identifiers[kind]).index(value["id"]) + 1
                        replacement = f"{kind.title()} {index:02}"
                    if replacement in display_reverse and display_reverse[replacement] != item:
                        raise ValueError("Fictional display names must have an unambiguous inverse")
                    display_reverse[replacement] = item
                    result[key] = replacement
            else:
                result[key] = transform(item, target, reverse)
        return result

    fictional = transform(source)
    restored = transform(fictional, reverse=True)
    assert restored == source, "Inverse mapping must preserve every value, omission and array order before upgrade"
    for mapping in mappings.values():
        assert list(mapping.values()) == sorted(mapping.values()), "Identifier ordering must be preserved"
    evidence = {"sample": identifier, "sourceSha256": hashlib.sha256(canonical(source)).hexdigest(),
                "inverseSha256": hashlib.sha256(canonical(restored)).hexdigest(),
                "inverseExact": True, "identifierOrderPreserved": True, "arrayOrderPreserved": True,
                "sourceCatalog": source["catalogVersion"], "packagedCatalog": 9,
                "mappedIdentifierCounts": {kind: len(mapping) for kind, mapping in sorted(mappings.items())}}
    fictional["catalogVersion"] = 9
    destination.mkdir(parents=True, exist_ok=True)
    (destination / f"{identifier}-definition.json").write_text(
        json.dumps(fictional, ensure_ascii=False, indent=2) + "\n")
    return evidence


if __name__ == "__main__":
    assert len(NAMES) == len(set(NAMES)) >= 51
    import argparse
    import tempfile
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Reproduce inverse/order/upgrade evidence without writing samples')
    args = parser.parse_args()
    if args.check:
        with tempfile.TemporaryDirectory(prefix='demo-anonymization-') as directory:
            destination = Path(directory)
            evidence = [generate(*sample, destination=destination) for sample in SAMPLES]
            assert evidence == json.loads((DESTINATION / 'generation-evidence.json').read_text())
            for identifier, _, _ in SAMPLES:
                name = f'{identifier}-definition.json'
                assert (destination / name).read_bytes() == (DESTINATION / name).read_bytes(), name
            print('PASS: exact inverse maps, identifier ordering, array order and current packaged definitions for all three samples')
    else:
        evidence = [generate(*sample) for sample in SAMPLES]
        (DESTINATION / "generation-evidence.json").write_text(json.dumps(evidence, indent=2) + "\n")
