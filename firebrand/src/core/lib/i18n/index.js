export default from './localized';

export type Localized = {
  t: (message: string, plural: ?string, count: ?number) => string;
};
