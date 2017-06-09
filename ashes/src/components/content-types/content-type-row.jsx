/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import MultiSelectRow from 'components/table/multi-select-row';
import OriginType from 'components/common/origin-type';

type Props = {
  contentType: Object,
  columns: Columns,
  params: Object,
};

const ContentTypeRow = (props: Props) => {
  const { contentType, columns, params } = props;

  const setCellContents = (contentType: Object, field: string) => {
    if (field === 'originType') {
      return (
        <OriginType value={contentType} />
      );
    }

    return _.get(contentType, field);
  };

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="contenttype"
      linkParams={{contentType: contentType.code}}
      row={contentType}
      setCellContents={setCellContents}
      params={params} />
  );
};

export default ContentTypeRow;
