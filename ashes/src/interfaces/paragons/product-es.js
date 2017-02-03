
import type { TSearchViewAlbum } from './album-es';

type MaybeString = ?string;

export type TSearchViewProduct = {
  id: number,
  productId: number,
  slug: MaybeString,
  context: string,
  scope: MaybeString,
  title: string,
  description: MaybeString,
  skus: Array<string>,
  tags: Array<string>,
  activeFrom: MaybeString,
  activeTo: MaybeString,
  archivedAt: MaybeString,
  externalId: MaybeString,
  albums: Array<TSearchViewAlbum>,
}
