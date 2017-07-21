// @flow

import _ from 'lodash';
import React, { Element } from 'react';
import { getTaxonValue } from 'paragons/taxons';
import classnames from 'classnames';

import specStyles from './specifications.css';
import descStyles from './product-description.css';

const styles = {
  ...specStyles,
  ...descStyles,
};

import type { Sku } from 'types/sku';

type BaseProps = {
  sku: Sku,
}

type SpecificationProps = {
  title: string,
  children?: Element<*>,
  className?: string,
  inline?: boolean,
}

const Specification = (props: SpecificationProps) => {
  const className = classnames(props.className, {
    [styles.inline]: props.inline,
  });
  return (
    <div styleName="spec" className={className}>
      <div styleName="spec-title">{props.title}</div>
      <div styleName="spec-content">{props.children}</div>
    </div>
  );
};

function getAttr(props: BaseProps, name: string) {
  return _.get(props.sku, `attributes.${name}.v`);
}

const Dimensions = (props: BaseProps) => {
  const width = getAttr(props, 'width');
  if (!width) return null;

  const height = getAttr(props, 'height');
  const depth = getAttr(props, 'depth');

  return (
    <Specification title="Dimensions" styleName="dimensions">
      <span>H: {height}</span>
      <span>W: {width}</span>
      <span>D: {depth}</span>
    </Specification>
  );
};

const Weight = (props: BaseProps) => {
  const weight = getAttr(props, 'weight');
  if (!weight) return null;

  return (
    <Specification title="Weight" inline>
      {weight}lbs
    </Specification>
  );
};

const Capacity = (props: BaseProps) => {
  const capacity = getAttr(props, 'Capacity');
  if (!capacity) return null;

  return (
    <Specification title="Capacity" inline>
      {capacity}L
    </Specification>
  );
};

const PrimaryMaterial = (props: BaseProps) => {
  const material = getTaxonValue(props.sku, 'material');
  if (!material) return null;

  return (
    <Specification title="Primary Material">
      {material}
    </Specification>
  );
};

function wrapListContent(html) {
  if (typeof html === 'string' && html.indexOf('<li') != -1 && html.indexOf('<ul') == '-1') {
    return `<ul>${html}</ul>`;
  }
  return html;
}

const Features = (props: BaseProps) => {
  const exteriorFeatures = getAttr(props, 'Exterior Features');
  const interiorFeatures = getAttr(props, 'Interior Features');

  if (!exteriorFeatures && !interiorFeatures) return null;

  return (
    <div styleName="features-list">
      {interiorFeatures && <Specification title="Interior Features" styleName="features">
        <div
          dangerouslySetInnerHTML={{__html: wrapListContent(interiorFeatures)}}
        />
      </Specification>}
      {exteriorFeatures && <Specification title="Exterior Features" styleName="features">
        <div
          dangerouslySetInnerHTML={{__html: wrapListContent(exteriorFeatures)}}
        />
      </Specification>}
    </div>
  );
};

export default function renderSpecifications(sku: Sku) {
  return (
    <div styleName="desc-content">
      <Dimensions sku={sku} />
      <div>
        <Weight sku={sku} />
        <Capacity sku={sku} />
      </div>
      <PrimaryMaterial sku={sku} />
      <Features sku={sku} />
      <div styleName="info-block">
        <a
          styleName="info-link"
          href="http://s7d2.scene7.com/is/content/Tumi/Documents/How_do_we_measure.pdf"
          title="How do we measure ?"
          rel="noopener noreferrer"
          target="_blank"
        >
          How do we measure ?
        </a>
      </div>
    </div>
  );
}
