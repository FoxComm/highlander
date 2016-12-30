export function randomInclusive(min: number, max: number) {
  min = Math.ceil(min);
  max = Math.floor(max);
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

export function randomIndex(max: number) {
  return randomInclusive(1, max);
}

export function withProbability(p: number) {
  return Math.random() <= p;
}

/*
*   n: max number of tries
*   returns i with probability (1-p)^i * p for 0 <= i < n
*   returns n with probability (1-p)^n
*/
export function firstBinom(p: number, n: number = Infinity) {
  for(let i = 0; i < n; i++){
    if(withProbability(p)) return i;
  }
  return n;
}
