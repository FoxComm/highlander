/* @flow */

// libs
import React, { Component } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// paragons
import { fieldTypes } from 'paragons/cms';

// styles
import styles from './cms.css';

type Props = {
  blocks: Array<any>,
};

const renderStatic = (blocks: Array<any>) => {
  return _.map(blocks, block => {
    return renderBlock(block);
  });
};

const renderBlock = (block) => {
  switch (block.type) {
    case fieldTypes.TITLE:
      return (
        <h2 styleName="title" key={block.id}>{block.content}</h2>
      );
    case fieldTypes.PARAGRAPH:
      return (
        <p
          styleName="paragraph"
          key={block.id}
          dangerouslySetInnerHTML={{__html: block.content}}
        />
      );
    case fieldTypes.PARAGRAPH_TITLE:
      return (
        <p>
          <strong styleName="paragraph-title" key={block.id}>{block.content}</strong>
        </p>
      );
    case fieldTypes.WELL:
      return (
        <div styleName="well" key={block.id}>
          {renderStatic(block.content)}
        </div>
      );
    case fieldTypes.EMAIL:
      return <a href={`mailto:${block.content}`} styleName="link">{block.content}</a>;
    case fieldTypes.PHONE:
      return <a href={`tel:${block.content}`} styleName="link">{block.content}</a>;
    default:
      return block.content;
  }
};

const PageBody = (props: Props) => {
  return (
    <div styleName="page-content">
      {renderStatic(props.blocks)}
    </div>
  );
};

export default PageBody;
