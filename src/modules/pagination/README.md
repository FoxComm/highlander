# Pagination & fetch logic for redux modules

Core functionality located in `index.js` and this functionality designed to be maximum flexibility.

For more simple cases use one of following schemas:

* `index.js` - default export from this file is most simple case: you have static url and flat store for data.
* `flatStore.js` - For case when you have dynamic url,
  but you do not need to save data from different URLs, in different places.
* `structuredStore.js` - For case when you have dynamic url,
   and you need to save data from different URLs, in different places.
