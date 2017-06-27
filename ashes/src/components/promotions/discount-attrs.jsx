/* @flow */
import _ from 'lodash';
import React from 'react';

import styles from './attrs-edit.css';

import * as widgets from './widgets';
import { Dropdown } from '../dropdown';
import { FormFieldError } from '../forms';

import type { ItemDesc, DiscountRow, DescriptionType, Context } from './types';

const renderers = {
  type(item: ItemDesc, context: Context, dropdownId: Props) {
    const typeItems = _.map(context.root, entry => [entry.type, entry.title]);

    return (
      <Dropdown
        id={dropdownId}
        styleName="type-chooser"
        items={typeItems}
        value={context.type}
        onChange={context.setType}
      />
    );
  },
  widget(item: ItemDesc, context: Context) {
    const widgetComponent = widgets[item.widget];
    const props = {...item, context};
    const element = React.createElement(widgetComponent, props);

    if (item.template) {
      return item.template({children: element});
    }

    return element;
  },
  title(item: ItemDesc) {
    return <strong>{item.title}</strong>;
  }
};

type Props = {
  onChange: (attrs: Object) => any;
  attr: string;
  descriptions: Array<DescriptionType>;
  discount: Object;
  dropdownId: string;
  blockId: string;
};

const DiscountAttrs = (props: Props) => {
  const discount = props.discount;

  const attrs = _.get(discount, `attributes.${props.attr}.v`, {});
  const discountType = Object.keys(attrs)[0];
  const discountParams = attrs[discountType] || {};

  const currentDescription =
    _.find(props.descriptions, entity => entity.type === discountType) || props.descriptions[0];

  const setParams = (params: Object) => {
    const key = _.keys(params)[0];
    const value = params[key];
    props.onChange({
      [discountType]: {
        ...discountParams,
        ...params,
      },
    }, {
      discountType,
      key,
      value,
    });
  };
  const setType = (type: any) => {
    const newDiscountParams = attrs[type] || _.find(props.descriptions, {type}).default || {};
    props.onChange({
      [type]: newDiscountParams
    });
  };

  const context = {
    type: discountType,
    params: discountParams,
    root: props.descriptions,
    setType,
    setParams,
  };

  const renderContentRow = (row: DiscountRow, i) => {
    return (
      <div styleName="form-row" key={`form-row-${i}`}>
        {_.map(row, (item: ItemDesc, i) => {
          const error = _.get(props, `errors[${discountType}][${item.name}]`, false);
          const type: string = item.type || 'widget';
          const renderer = renderers[type];
          if (!renderer) return null;

          return (
            <div key={i}>
              {renderer(item, context, props.dropdownId)}
              {error && <FormFieldError error={error} />}
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div id={props.blockId}>
      {_.map(currentDescription.content, renderContentRow)}
    </div>
  );
};

export default DiscountAttrs;
