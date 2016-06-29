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
import { ContentBlock, ContentState, Editor, EditorState, RichUtils } from 'draft-js';
import { Dropdown, DropdownItem } from '../dropdown';
import StyleButton from './style-button';

type Props = {
  label?: string,
  onChange: (content: string) => void,
  value: ?string,
  className?: string,
};

type State = { editorState: Object };
type Style = { label: string, style: string };

const headerStyles = [
  { label: 'H1', style: 'header-one' },
  { label: 'H2', style: 'header-two' },
  { label: 'H3', style: 'header-three' },
  { label: 'H4', style: 'header-four' },
  { label: 'H5', style: 'header-five' },
  { label: 'H6', style: 'header-six' },
];

const listStyles = [
  { label: 'icon-bullets', style: 'unordered-list-item' },
  { label: 'icon-numbers', style: 'ordered-list-item' },
];

const inlineStyles = [
  { label: 'icon-bold', style: 'BOLD' },
  { label: 'icon-italic', style: 'ITALIC' },
  { label: 'icon-underline', style: 'UNDERLINE' },
];

function stopPropagation(event: Object) {
  event.preventDefault();
  event.stopPropagation();
}

export default class RichTextEditor extends Component<void, Props, State> {
  props: Props;

  state: State = {
    editorState: this.valueToEditorState(this.props.value),
  };

  valueToEditorState(value: ?string): any {
    return value ? EditorState.createWithContent(stateFromHTML(value)) : EditorState.createEmpty();
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.value != nextProps.value) {
      this.setState({
        editorState: this.valueToEditorState(nextProps.value),
      });
    }
  }

  get headerButtons(): Element {
    const { editorState } = this.state;
    const selection = editorState.getSelection();
    const blockType = editorState
      .getCurrentContent()
      .getBlockForKey(selection.getStartKey())
      .getType();

    return (
      <div className="fc-rich-text-editor__command-set">
        <Dropdown
          className="fc-rich-text-editor__command-headers"
          placeholder={<i className="icon-size" />}
          onChange={this.handleBlockTypeChange}
          value={blockType}
          items={headerStyles.map(t => [t.style, t.label])}
        />
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


    const isActive = style => style == blockType;
    return this.renderStyleButtons(listStyles, isActive, this.handleBlockTypeChange);
  }

  get styleButtons(): Element {
    const currentStyle = this.state.editorState.getCurrentInlineStyle();
    const isActive = style => currentStyle.has(style);
    return this.renderStyleButtons(inlineStyles, isActive, this.handleStyleClick);
  }

  blockStyleFn(contentBlock: ContentBlock) {
    const type = contentBlock.getType();
    switch (type) {
      case 'ordered-list-item':
        return 'fc-rich-text-editor__ol-block-style';
      case 'unordered-list-item':
        return 'fc-rich-text-editor__ul-block-style';
    }
  }

  @autobind
  handleBlockTypeChange(blockType: string) {
    this.handleChange(RichUtils.toggleBlockType(this.state.editorState, blockType));
  }

  @autobind
  handleStyleClick(style: string) {
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

  renderStyleButtons(styles: Array<Style>,
                     isActive: (v: string) => boolean,
                     onClick: (v: string) => void): Element {
    const buttons = styles.map(type => {
      return (
        <StyleButton
          isActive={isActive(type.style)}
          labelIcon={type.label}
          onClick={onClick}
          style={type.style}
          key={type.label}
        />
      );
    });

    return <div className="fc-rich-text-editor__command-set">{buttons}</div>;
  }

  render(): Element {
    const { editorState } = this.state;
    return (
      <div className={classNames('fc-rich-text-editor', this.props.className)}>
        {this.props.label && <div className="fc-rich-text-editor__label">{this.props.label}</div>}
        <div className="fc-rich-text-editor__command-bar">
          {this.headerButtons}
          {this.styleButtons}
          {this.listButtons}
        </div>
        <div className="fc-rich-text-editor__content">
          <Editor
            editorState={editorState}
            blockStyleFn={this.blockStyleFn}
            handleKeyCommand={this.handleKeyCommand}
            onBlur={this.handleBlur}
            onChange={this.handleChange} />
        </div>
      </div>
    );
  }
}
