export default from './localized';

export type Localized = {
  t: (messsage: string, plural: ?string, count: ?number) => string;
};
