/* @flow */

import React, { Element } from 'react';
import type { SavedSearch } from 'paragons/saved-search';
import type { SearchResult } from 'paragons/search-result';

type Props = {
  data: SearchResult<any>,
  searches: Array<SavedSearch>,
  title: string,
};

// ListPage is the v2 of the old list page component. It contains the general
// list page paradigm that we use on the site: a header with a count, the
// optional ability to create a new entity, the main grid view, and a set of
// tabs for alternate views.
const ListPage = (props: Props): Element => {
  return (
    <div>
      I'm a list page!
    </div>
  );
};

export default ListPage;
