class Unauthorized extends Error {
  constructor() {
    super('Unauthorized');
    this.message = 'Unauthorized';
    this.status = 401;
  }
}

module.exports = Unauthorized;
