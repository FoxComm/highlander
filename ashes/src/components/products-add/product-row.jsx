/* @flow */

//libs
import { get, reduce, size } from 'lodash';
import React, { Element } from 'react';

// components
import { TableRow, TableCell } from 'components/table';
import { Button } from 'components/core/button';

// styles
import styles from './products-add.css';

type Params = {
  inProgress: boolean,
  isNew: boolean,
  onAdd: (product: Product) => any,
};

type Props = {
  product: Product,
  columns?: Columns,
  params: Params,
};

function setCellContents(product: Product, field: string, params: Params) {
  switch (field) {
    case 'add':
      return (
        <Button
          className={styles.button}
          isLoading={params.inProgress}
          onClick={() => params.onAdd(product)}
          children={params.isNew ? 'Add' : 'Added'}
          disabled={!params.isNew}
        />
      );
    case 'skus':
      return size(get(product, 'skus'));
    case 'image':
      return get(product, ['albums', 0, 'images', 0, 'src']);
    default:
      return get(product, field);
  }
}

const ProductRow = (props: Props) => {
  const { product, columns, params } = props;

  const cells = reduce(columns, (result: Array<Element<*>>, col: Column) => {
    result.push(
      <TableCell column={col} key={col.field}>
        {setCellContents(product, col.field, params)}
      </TableCell>
    );

    return result;
  }, []);

  return (
    <TableRow>{cells}</TableRow>
  );
};

export default ProductRow;
