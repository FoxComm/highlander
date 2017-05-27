/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from 'components/table/multi-select-row';

// styles
import styles from './catalog-row.css';

type Props = {
  catalog: Catalog,
  columns?: Columns,
  params: Object,
}

const CatalogRow = (props: Props) => {
  const { catalog, columns, params } = props;

  const commonParams = {
    columns,
    row: catalog,
    setCellContents: (catalog: Object, field: string) => _.get(catalog, field),
    params,
  };

  return (
    <MultiSelectRow
      {...commonParams}
    />
  );
};

export default CatalogRow;
