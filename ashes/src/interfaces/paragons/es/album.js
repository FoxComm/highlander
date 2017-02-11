
type TImage = {
  alt: string,
  baseurl: ?string,
  src: string,
  title: string,
}

export type TSearchViewAlbum = {
  name: string,
  images: Array<TImage>
}
