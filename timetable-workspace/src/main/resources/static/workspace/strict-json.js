export function strictParse(text){
 const value=JSON.parse(text),tokens=[...text.matchAll(/"(?:\\.|[^"\\])*"|[{}\[\]:,]|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?|true|false|null/g)],stack=[];
 for(let i=0;i<tokens.length;i++){const token=tokens[i][0];if(token==='{')stack.push(new Set());else if(token==='[')stack.push(null);else if(token==='}'||token===']')stack.pop();else if(token.startsWith('"')&&tokens[i+1]?.[0]===':'){const key=JSON.parse(token),seen=stack.at(-1);if(seen?.has(key))throw new Error(`Duplicate field ${key} at character ${tokens[i].index+1}.`);seen?.add(key);}}
 return value;
}
