/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import { stateFromHTML } from 'draft-js-import-html';
import { stateToHTML } from 'draft-js-export-html';
import _ from 'lodash';

// components
import { ContentState, Editor, EditorState, RichUtils } from 'draft-js';
import { Dropdown, DropdownItem } from '../dropdown';

type Props = {
  label?: string,
  onChange: (content: string) => void,
  value: ?string,
};

type State = { editorState: Object };

const headerStyles = [
  { label: 'H1', style: 'header-one' },
  { label: 'H2', style: 'header-two' },
  { label: 'H3', style: 'header-three' },
  { label: 'H4', style: 'header-four' },
  { label: 'H5', style: 'header-five' },
  { label: 'H6', style: 'header-six' },
];

const listStyles = [
  { label: 'UL', style: 'unordered-list-item' },
  { label: 'OL', style: 'ordered-list-item' },
];

const inlineStyles = [
  { label: 'B', style: 'BOLD' },
  { label: 'I', style: 'ITALIC' },
  { label: 'U', style: 'UNDERLINE' },
];

function stopPropagation(event: Object) {
  event.preventDefault();
  event.stopPropagation();
}

export default class RichTextEditor extends Component<void, Props, State> {
  state: State;

  constructor(props: Props) {
    super(props);

    let editorState = EditorState.createEmpty();
    if (this.props.value) {
      const contentState = stateFromHTML(this.props.value);
      editorState = EditorState.createWithContent(contentState);
    }
    this.state = { editorState };
  }

  get headerButtons(): Element {
    const { editorState } = this.state;
    const selection = editorState.getSelection();
    const blockType = editorState
      .getCurrentContent()
      .getBlockForKey(selection.getStartKey())
      .getType();

    const items = headerStyles.map(t => <DropdownItem value={t.style}>{t.label}</DropdownItem>);

    return (
      <div className="fc-rich-text-editor__command-set">
        <Dropdown 
          className="fc-rich-text-editor__command-headers"
          placeholder="A"
          onChange={this.handleBlockTypeChange}
          value={blockType}>
          {items}
        </Dropdown>
      </div>
    );
  }

  get listButtons(): Element {
    const { editorState } = this.state;
    const selection = editorState.getSelection();
    const blockType = editorState
      .getCurrentContent()
      .getBlockForKey(selection.getStartKey())
      .getType();

    const buttons = listStyles.map(type => {
      const className = classNames('fc-rich-text-editor__command-button', {
        '_active': type.style === blockType,
      });

      return (
        <button
          className={className}
          onClick={(e) => this.handleListTypeClick(type.style, e)}
          onMouseDown={stopPropagation}
          onMouseUp={stopPropagation}>
          {type.label}
        </button>
      );
    });

    return <div className="fc-rich-text-editor__command-set">{buttons}</div>;
  }

  get styleButtons(): Element {
    const currentStyle = this.state.editorState.getCurrentInlineStyle();
    const buttons = inlineStyles.map(type => {
      const className = classNames('fc-rich-text-editor__command-button', {
        '_active': currentStyle.has(type.style)
      });

      return (
        <button
          className={className}
          onClick={(e) => this.handleStyleClick(type.style, e)}
          onMouseDown={stopPropagation}
          onMouseUp={stopPropagation}>
          {type.label}
        </button>
      );
    });

    return <div className="fc-rich-text-editor__command-set">{buttons}</div>;
  }

  @autobind
  handleBlockTypeChange(blockType: string) {
    this.handleChange(RichUtils.toggleBlockType(this.state.editorState, blockType));
  }

  @autobind
  handleListTypeClick(blockType: string, event: Object) {
    stopPropagation(event);
    this.handleBlockTypeChange(blockType);
  }

  @autobind
  handleStyleClick(style: string, event: Object) {
    stopPropagation(event);
    this.handleChange(RichUtils.toggleInlineStyle(this.state.editorState, style));
  }

  @autobind
  handleChange(editorState: Object) {
    this.setState({ editorState });
  }

  @autobind
  handleBlur() {
    const contentState: ContentState = this.state.editorState.getCurrentContent();
    const rawHTML: string = stateToHTML(contentState);
    this.props.onChange(rawHTML);
  }

  @autobind
  handleKeyCommand(command: Object): boolean {
    const newState = RichUtils.handleKeyCommand(this.state.editorState, command);
    if (newState) {
      this.handleChange(newState);
      return true;
    }

    return false;
  }

  render(): Element {
    const { editorState } = this.state;
    return (
      <div className="fc-rich-text-editor">
        {this.props.label && <div className="fc-rich-text-editor__label">{this.props.label}</div>}
        <div className="fc-rich-text-editor__command-bar">
          {this.headerButtons}
          {this.styleButtons}        
          {this.listButtons}
        </div>
        <div className="fc-rich-text-editor__content">
          <Editor
            editorState={editorState}
            handleKeyCommand={this.handleKeyCommand}
            onBlur={this.handleBlur}
            onChange={this.handleChange} />
        </div>
      </div>
    );
  }
}
