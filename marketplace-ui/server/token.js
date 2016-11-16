import localStorage from 'localStorage';

export default function *token(next) {
  localStorage.setItem('jwt', this.cookies.get('JWT'));

  yield next;
}
