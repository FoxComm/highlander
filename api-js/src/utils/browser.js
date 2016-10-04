export const isBrowser = new Function("try {return this===window;}catch(e){ return false;}");

export function loadScript(path) {
  return new Promise((resolve, reject) => {
    if (document === void 0) {
      return;
    }

    const script = document.createElement('script');
    script.onload = resolve;
    script.onerror = reject;

    script.src = path;
    document.getElementsByTagName('head')[0].appendChild(script);
  });
}