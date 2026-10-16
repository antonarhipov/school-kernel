import test from 'node:test';
import assert from 'node:assert/strict';
import { createTimetableStorage, deleteLocalTimetableData, storageKey } from '../../timetable-workspace/src/main/resources/static/workspace/timetable-storage.js';
import { prefix, useKey } from '../../timetable-workspace/src/main/resources/static/workspace/demo-definitions.js';
const storage = () => {
  const values = new Map();
  return { get length() { return values.size; }, key: index => [...values.keys()][index] ?? null,
    getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) };
};
test('draft identities and exact optional values survive independent browser reopen', () => {
  const store = storage(), repository = createTimetableStorage(store);
  repository.write(s => { s.drafts['full/a'] = { changes: { lesson: { roomId: 'lab', teacherId: 'mari' } }, name: 'Alternate' }; });
  repository.write(s => { s.drafts['two-class/a'] = { changes: { lesson: { periodId: 'tue-2' } } }; });
  assert.deepEqual(createTimetableStorage(store).state().drafts, repository.state().drafts);
  const copy = repository.state(); copy.drafts['full/a'].name = 'mutated caller';
  assert.equal(repository.state().drafts['full/a'].name, 'Alternate');
});
test('quota failure preserves last complete durable value and current in-memory draft', () => {
  const store = storage(), repository = createTimetableStorage(store);
  repository.write(s => { s.drafts['full/a'] = { name: 'before' }; });
  const previous = store.getItem(storageKey);
  store.setItem = () => { throw new Error('QuotaExceededError'); };
  assert.throws(() => repository.write(s => { s.drafts['full/a'].name = 'edited'; }), /Nothing was saved/);
  assert.equal(store.getItem(storageKey), previous);
  assert.equal(repository.state().drafts['full/a'].name, 'edited');
});
test('another tab cannot be overwritten by a stale browser catalog', () => {
  const store = storage(), first = createTimetableStorage(store), second = createTimetableStorage(store);
  first.write(s => { s.drafts['full/a'] = { name: 'first owner tab' }; });
  assert.throws(() => second.write(s => { s.drafts['full/b'] = { name: 'second tab edit' }; }), /Another tab/);
  assert.equal(createTimetableStorage(store).state().drafts['full/a'].name, 'first owner tab');
  assert.equal(second.state().drafts['full/b'].name, 'second tab edit');
});
test('corrupt or unsupported storage is retained without overwriting it', () => {
  const store = storage(); store.setItem(storageKey, '{unfinished');
  const repository = createTimetableStorage(store);
  assert.equal(repository.notices.length, 1);
  assert.throws(() => repository.write(s => { s.selected = { schoolId: 'full' }; }), /only in memory/);
  assert.equal(store.getItem(storageKey), '{unfinished');
});
test('atomic version save failure retains the source draft and adds no phantom saved version', () => {
  const store = storage(), repository = createTimetableStorage(store);
  repository.write(s => { s.drafts['full/a'] = { name: 'My alternative' }; });
  const before = repository.state(), durable = store.getItem(storageKey);
  store.setItem = () => { throw new Error('QuotaExceededError'); };
  assert.throws(() => repository.write(s => {
    s.timetables.full = { saved: { name: 'My alternative' } }; delete s.drafts['full/a'];
  }, { atomic: true }), /Nothing was saved/);
  assert.deepEqual(repository.state(), before);
  assert.equal(store.getItem(storageKey), durable);
});

test('cleanup removes current and legacy artifacts including corrupt entries, preserving unrelated values and stale-tab guards', () => {
  const store = storage(), repository = createTimetableStorage(store);
  repository.write(s => { s.schools.copy = { name: 'Copy' }; s.timetables.full = { saved: { name: 'Alternative' } }; s.drafts['full/a'] = { name: 'Draft' }; });
  store.setItem(prefix + 'legacy', '{unreadable'); store.setItem(useKey, '{old reference');
  store.setItem('school-kernel.inspection.full', 'view preference'); store.setItem('unrelated', 'keep exactly');
  deleteLocalTimetableData(store);
  assert.equal(store.getItem(storageKey), null); assert.equal(store.getItem(prefix + 'legacy'), null); assert.equal(store.getItem(useKey), null);
  assert.equal(store.getItem('school-kernel.inspection.full'), 'view preference'); assert.equal(store.getItem('unrelated'), 'keep exactly');
  assert.throws(() => repository.write(s => { s.selected = { schoolId: 'copy' }; }), /Another tab/);
  assert.deepEqual(createTimetableStorage(store).state().schools, {});
  store.setItem(storageKey, '{corrupt root'); deleteLocalTimetableData(store);
  assert.equal(store.getItem(storageKey), null);
});

test('failed cleanup restores the exact raw values and allows retry without clearing unrelated storage', () => {
  const store = storage(); store.setItem(storageKey, '{exact unfinished root'); store.setItem(prefix + 'legacy', 'exact legacy');
  store.setItem('unrelated', 'keep'); const remove = store.removeItem; let calls = 0;
  store.removeItem = key => { if (++calls === 2) throw new Error('SecurityError'); remove(key); };
  assert.throws(() => deleteLocalTimetableData(store), /could not be deleted.*retained/);
  assert.equal(store.getItem(storageKey), '{exact unfinished root'); assert.equal(store.getItem(prefix + 'legacy'), 'exact legacy'); assert.equal(store.getItem('unrelated'), 'keep');
  store.removeItem = remove; deleteLocalTimetableData(store);
  assert.equal(store.getItem(storageKey), null); assert.equal(store.getItem(prefix + 'legacy'), null); assert.equal(store.getItem('unrelated'), 'keep');
});

test('cleanup reports possible partial deletion when the browser also refuses restoration', () => {
  const store = storage(); store.setItem(storageKey, 'saved root'); store.setItem(prefix + 'legacy', 'saved legacy'); store.setItem('unrelated', 'keep');
  const remove = store.removeItem;
  store.removeItem = key => { if (key === prefix + 'legacy') throw new Error('refused'); remove(key); };
  store.setItem = () => { throw new Error('restoration refused'); };
  assert.throws(() => deleteLocalTimetableData(store), /Some local data may have been deleted/);
  assert.equal(store.getItem('unrelated'), 'keep'); assert.equal(store.getItem(prefix + 'legacy'), 'saved legacy');
});
