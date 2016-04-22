
import _ from 'lodash';
import React, { Element } from 'react';

import styles from './attrs-edit.css';

import * as widgets from './widgets';
import { Dropdown } from '../dropdown';

/*
{
  type: 'itemsSelectPercentOff',
  title: 'Percent off select items',
  content: [
  [
    {type: 'type'},
    {
      name: 'discount',
      widget: 'percent',
      template: props => <div>Get {props.children} off discounted items.</div>
    }
  ],
  [
    {type: 'title', title: 'Discounted Items'},
    {
      name: 'references',
      widget: 'selectProducts',
      label: 'Discount the items'
    }
  ]
]
}*/

type ItemDesc = {
  type?: string;
  name?: string;
  widget?: string;
  template?: (props: Object) => Element;
}

const renderers = {
  type(item: ItemDesc, context) {
    const typeItems = _.map(context.root, entry => [entry.type, entry.title]);

    return (
      <Dropdown
        styleName="type-chooser"
        items={typeItems}
        value={context.type}
        onChange={context.setType}
      />
    );
  },
  widget(item:ItemDesc, context) {
    const widgetComponent = widgets[item.widget];
    const props = {...item, context};
    const element = React.createElement(widgetComponent, props);

    if (item.template) {
      return item.template({children: element});
    }

    return element;
  },
  title(item: ItemDesc, context) {
    return <strong>{item.title}</strong>;
  }
};

type DiscountRow = Array<ItemDesc>;

type DescriptionType = {
  type: string;
  title: string;
  content?: Array<DiscountRow>;
}

type Props = {
  onChange: (attrs: Object) => any;
  attr: string;
  descriptions: Array<DescriptionType>;
  discount: Object;
};

const DiscountAttrs = (props: Props) => {
  const discount = props.discount;

  const attrs = _.get(discount, `form.attributes.${props.attr}`, {});
  const discountType = Object.keys(attrs)[0];
  const discountParams = attrs[discountType] || {};

  const currentDescription =
    _.find(props.descriptions, entity => entity.type === discountType) || props.descriptions[0];

  const setParams = (params: Object) => {
    props.onChange({
      [discountType]: {
        ...discountParams,
        ...params,
      },
    });
  };

  const setType = (type: any) => {
    props.onChange({
      [type]: discountParams,
    });
  };

  const context = {
    type: discountType,
    params: discountParams,
    root: props.descriptions,
    setType,
    setParams,
  };

  const renderContentRow = (row: DiscountRow) => {
    return (
      <div styleName="form-row">
        {_.map(row, (item: ItemDesc) => {
          const type: string = item.type || 'widget';
          const renderer = renderers[type];
          if (!renderer) return null;

          return renderer(item, context);
        })}
      </div>
    );
  };

  return (
    <div>
      {_.map(currentDescription.content, renderContentRow)}
    </div>
  );
};

export default DiscountAttrs;
