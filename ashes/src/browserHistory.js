let history = null;

export function setHistory(h) {
  history = h;
};

export function transitionTo(name, params) {
  return history.push({
    name: name,
    params: params
  });
};

export function transitionToLazy(name, params = {}) {
  return function () {
    return transitionTo(name, params);
  };
};
