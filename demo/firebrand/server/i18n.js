const fs = require('fs');
const path = require('path');

const filePath = language => path.join(process.cwd(), `src/i18n/${language}.json`);

function *getTranslation(language: string, defaultLanguage: string): Promise {
  const options = {encoding: 'utf8'};

  return new Promise(resolve => {
    fs.readFile(filePath(language), options, (err, data) => {
      const result = {};
      result.language = err ? defaultLanguage : language;
      result.translation = JSON.parse(err ? fs.readFileSync(filePath(defaultLanguage), options) : data);
      resolve(result);
    });
  });
}

export default function *loadI18n(next) {
  const defaultLanguage = process.env.FIREBRAND_LANGUAGE;
  const preferredLanguage = this.request.header['accept-language'] || defaultLanguage;

  this.state.i18n = yield getTranslation(preferredLanguage, defaultLanguage);

  yield next;
}
