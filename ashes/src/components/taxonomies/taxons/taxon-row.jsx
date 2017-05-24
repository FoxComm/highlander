/* @flow */

import React from 'react';

// libs
import { activeStatus } from 'paragons/common';

// components
import MultiSelectRow from 'components/table/multi-select-row';
import RoundedPill from 'components/rounded-pill/rounded-pill';

type Props = {
  taxon: TaxonResult,
  columns?: Array<Object>,
  params: Object,
};

const TaxonRow = (props: Props) => {
  const { taxon, ...rest } = props;

  const setCellContents = (taxon: TaxonResult, field: string) => {
    switch (field) {
      case 'state':
        return <RoundedPill text={activeStatus(taxon)} />;
      case 'productsCount':
        return 0; // TODO: fix after ES mapping update
      default:
        return taxon[field];
    }
  };

  return (
    <MultiSelectRow
      {...rest}
      row={taxon}
      setCellContents={setCellContents}
      linkTo="taxon-details"
      linkParams={taxon}
    />
  );
};

export default TaxonRow;
