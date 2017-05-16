try {
  rev = fs.readFileSync(path.resolve(__dirname, '.git-rev'), 'utf8').trim();
} catch (e) {
  rev = 'unknown';
}

process.env.API_URL = process.env.API_URL || 'http://localhost';
process.env.NODE_ENV = process.env.NODE_ENV || 'development';
process.env.GIT_REVISION = rev;
