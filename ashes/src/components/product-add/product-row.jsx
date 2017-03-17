/* @flow */

//libs
import { get, find, reduce, size } from 'lodash';
import React, { Element } from 'react';

// helpers
import { activeStatus, isArchived } from 'paragons/common';

// components
import { TableRow, TableCell } from 'components/table';
import { Button } from 'components/common/buttons';

// styles
import styles from './product-add.css';

type Params = {
  addState: AsyncState,
  addedProducts: Array<Product>,
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
      const isNew = !find(params.addedProducts, (p: Product) => p.productId === product.productId);

      return (
        <Button
          className={styles.button}
          isLoading={params.addState.inProgress}
          onClick={() => params.onAdd(product)}
          children={isNew ? 'Add' : 'Added'}
          disabled={!isNew}
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
