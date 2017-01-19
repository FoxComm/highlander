
const crypto = require('crypto');
const fs = require('fs-promise');
const path = require('path');

function md5(...args) {
  return crypto.createHash('md5').update(args.join('')).digest('hex');
}

function basename(req) {
  let name = req.url;

  if (req.method.toLowerCase() != 'get' || req.querystring || req.fields) {
    name = name + `_${md5(req.method, req.href, JSON.stringify(req.fields))}`;
  }

  return name;
}

class Cache {
  constructor(dir) {
    this.dir = dir;
  }

  replacementPath(req) {
    return path.join(this.dir, basename(req) + '.json');
  }

  dataPath(req) {
    return path.join(this.dir, basename(req) + '_data');
  }

  metaPath(req) {
    return path.join(this.dir, basename(req) + '_meta');
  }

  *writeMeta(req, res) {
    const meta = {
      headers: {}
    };
    Object.assign(meta.headers, res.headers);
    meta.status = res.statusCode;

    const cacheFile = this.metaPath(req);

    yield fs.ensureDir(path.dirname(cacheFile));
    return fs.writeFile(cacheFile, JSON.stringify(meta));
  }

  *writeData(req, res) {
    const cacheFile = this.dataPath(req);

    yield fs.ensureDir(path.dirname(cacheFile));
    return fs.writeFile(cacheFile, res.body);
  }

  applyMeta(meta, res) {
    Object.keys(meta.headers).map(name => {
      if (name != 'transfer-encoding') {
        res.set(name, meta.headers[name]);
      }
    });
    res.status = meta.status;
  }

  applyBody(body, res) {
    res.body = body;
  }

  *applyCache(req, res) {
    const meta = yield fs.readFile(this.metaPath(req));
    const data = yield fs.readFile(this.dataPath(req));

    this.applyMeta(JSON.parse(meta), res);
    this.applyBody(data, res);
  }

  exists(req) {
    return fs.exists(this.metaPath(req));
  }

  replacementExists(req) {
    return fs.exists(this.replacementPath(req));
  }
}

module.exports = Cache;