import { bytes, maxBytes } from './demo-definitions.js';

// Retain the latest text in memory; never clone/serialize the repository on every keystroke.
export function createDefinitionAutosave(save, oversized, delay = 300) {
  let pending = null, timer, blocked = false;
  const fits = text => text.length <= maxBytes && bytes(text) <= maxBytes;
  const cancelTimer = () => { clearTimeout(timer); timer = undefined; };
  function flush() {
    cancelTimer();
    if (!pending) return true;
    const value = pending;
    if (!fits(value.text)) { oversized(true); return false; }
    blocked = !save(value);
    if (!blocked) pending = null;
    return !blocked;
  }
  return {
    schedule(value) {
      cancelTimer();
      pending = value;
      const tooLarge = !fits(value.text);
      oversized(tooLarge);
      if (!tooLarge && !blocked) timer = setTimeout(flush, delay);
    },
    flush,
    cancel() { cancelTimer(); pending = null; blocked = false; }
  };
}
