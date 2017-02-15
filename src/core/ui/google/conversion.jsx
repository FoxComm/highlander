// @flow
import React from 'react';

type ConversionParams = {
  id: number,
  label: string,
  value: number,
  currency?: string,
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
  } = params;

  const url =
    `//www.googleadservices.com/pagead/conversion/${id}/` +
    `?label=${label}&` +
    `currency_code=${currency}&` +
    `value=${value}&` +
    `guid=ON&script=0`;

  const variables = (
    <script type="text/javascript" dangerouslySetInnerHTML={{
      __html: `/* <![CDATA[ */
        var google_conversion_id = ${id};
        var google_conversion_language = "en";
        var google_conversion_format = "3";
        var google_conversion_color = "ffffff";
        var google_conversion_label = "${label}";
        var google_remarketing_only = false;
        var google_conversion_currency = "${currency}";
        var google_conversion_value = ${value};
        /* ]]> */`,
    }}
    />
  );

  const conversionScript = (
    <script type="text/javascript" src="//www.googleadservices.com/pagead/conversion.js" />
  );

  const image = (
    <noscript>
      <img
        width="1"
        height="1"
        style={{ borderStyle: 'none' }}
        alt=""
        src={url}
      />
    </noscript>
  );

  return (
    <div>
      {variables}
      {conversionScript}
      {image}
    </div>
  );
};

export default Conversion;
