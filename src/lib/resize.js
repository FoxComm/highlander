const callbacks = [];
let running = false;

const notify = (event) => () => {
  callbacks.forEach((callback) => callback(event));
  running = false;
};

const resize = (event)=> {
  if (running) {
    return;
  }
  running = true;

  if (global.requestAnimationFrame) {
    global.requestAnimationFrame(notify(event));
  } else {
    setTimeout(notify(event), 66);
  }
};

export const addResizeListener = (callback) => {
  if (!callbacks.length) {
    window.addEventListener('resize', resize);
  }
  callbacks.push(callback);
};

export const removeResizeListener = (callback)=> {
  callbacks.splice(callbacks.indexOf(callback), 1);
  if (!callbacks.length) {
    window.removeEventListener('resize', resize);
  }
};
