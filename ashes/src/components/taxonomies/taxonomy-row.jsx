/* @flow */

import React, { Element } from 'react';

// libs
import { activeStatus, isArchived } from 'paragons/common';

// components
import MultiSelectRow from 'components/table/multi-select-row';
import { RoundedPill } from 'components/core/rounded-pill';

type Props = {
  taxonomy: TaxonomyResult,
  columns?: Columns,
  params: Object,
};

const TaxonomyRow = (props: Props) => {
  const { taxonomy, columns, params } = props;

  const setCellContents = (taxonomy: TaxonomyResult, field: string) => {
    switch(field) {
      case 'state':
        return (
          <RoundedPill text={activeStatus(taxonomy)} />
        );
      default:
        return taxonomy[field];
    }
  };

  const commonParams = {
    columns,
    row: taxonomy,
    setCellContents,
    params,
  };

  if (isArchived(taxonomy)) {
    return <MultiSelectRow {...commonParams} />;
  }

  return (
    <MultiSelectRow
      {...commonParams}
      linkTo="taxonomy-details"
      linkParams={{taxonomyId: taxonomy.taxonomyId, context: taxonomy.context}}
    />
  );
};

export default TaxonomyRow;
