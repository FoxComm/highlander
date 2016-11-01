/* @flow */

// libs
import React from 'react';
import _ from 'lodash';

// paragons
import { fieldTypes } from 'paragons/cms';

// styles
import styles from './cms.css';

type Props = {
  blocks: Array<any>,
};

const generateKey = (): string => {
  return Math.random().toString(36).substring(7).toUpperCase();
};

const renderBlock = (block) => {
  let wellContent = '';
  switch (block.type) {
    case fieldTypes.TITLE:
      return (
        <h2 styleName="title" key={generateKey()}>{block.content}</h2>
      );
    case fieldTypes.PARAGRAPH:
      return (
        <p
          styleName="paragraph"
          key={generateKey()}
          dangerouslySetInnerHTML={{__html: block.content}}
        />
      );
    case fieldTypes.PARAGRAPH_TITLE:
      return (
        <h2 key={generateKey()} styleName="paragraph-title">
          {block.content}
        </h2>
      );
    case fieldTypes.WELL:
      wellContent = renderStatic(block.content); // eslint-disable-line no-use-before-define
      return (
        <div styleName="well" key={generateKey()}>
          {wellContent}
        </div>
      );
    case fieldTypes.EMAIL:
      return (
        <a href={`mailto:${block.content}`} styleName="link" key={generateKey()}>
          {block.content}
        </a>
      );
    case fieldTypes.PHONE:
      return (
        <a href={`tel:${block.content}`} styleName="link" key={generateKey()}>
          {block.content}
        </a>
      );
    default:
      return block.content;
  }
};

const renderStatic = (blocks: Array<any>) => {
  return _.map(blocks, block => {
    return renderBlock(block);
  });
};

const PageBody = (props: Props) => {
  return (
    <div styleName="page-content">
      {renderStatic(props.blocks)}
    </div>
  );
};

export default PageBody;
