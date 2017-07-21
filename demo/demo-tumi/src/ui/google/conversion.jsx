// @flow
import _ from 'lodash';
import React from 'react';

type ConversionParams = {
  id: number,
  label: string,
  value: number,
  currency?: string,
  orderId?: string,
}

type Props = {
  params: ConversionParams,
}

const Conversion = (props: Props) => {
  const { params } = props;
  const value = (params.value).toFixed(2);

  const {
    label,
    currency = 'USD',
    id,
    orderId,
  } = params;

  const urlValues = _.compact([
    `label=${label}`,
    `currency_code=${currency}`,
    `value=${value}`,
    'guid=ON',
    orderId ? `oid=${orderId}` : null,
    'script=0',
  ]).join('&');

  const url = `//www.googleadservices.com/pagead/conversion/${id}/?${urlValues}`;

  return (
    <img
      width="1"
      height="1"
      alt=""
      style={{borderStyle: 'none'}}
      src={url}
    />
  );
};

export default Conversion;
