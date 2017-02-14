export default (keys, ...objects) => objects.map((object) => {
  const strippedObject = Object.assign({}, object);
  for (const key of keys) {
    delete strippedObject[key];
  }
  return strippedObject;
});
