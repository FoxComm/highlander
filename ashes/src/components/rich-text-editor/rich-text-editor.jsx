/**
 * @flow
 */

// libs
import _ from 'lodash';
import React, { Component, Element, PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';
import { stateFromHTML } from 'draft-js-import-html';
import { stateToHTML } from 'draft-js-export-html';
import { stateFromMarkdown } from 'draft-js-import-markdown';
import { stateToMarkdown } from 'draft-js-export-markdown';

// components
import { ContentBlock, ContentState, Editor, EditorState, RichUtils } from 'draft-js';
import { Dropdown } from '../dropdown';
import ToggleButton from './toggle-button';
import s from './rich-text-editor.css';

type Props = {
  label?: string,
  onChange: (content: string) => void,
  value: ?string,
  className?: string,
};

type State = {
  editorState: Object,
  contentType: string,
  richMode: boolean;
};
type ButtonData = { label: string, value: string, title?: string };

const headerStyles = [
  { label: 'H1', value: 'header-one' },
  { label: 'H2', value: 'header-two' },
  { label: 'H3', value: 'header-three' },
  { label: 'H4', value: 'header-four' },
  { label: 'H5', value: 'header-five' },
  { label: 'H6', value: 'header-six' },
];

const listStyles = [
  { label: 'icon-bullets', value: 'unordered-list-item', title: 'Unordered list' },
  { label: 'icon-numbers', value: 'ordered-list-item', title: 'Ordered list' },
];

const inlineStyles = [
  { label: 'icon-bold', value: 'BOLD', title: 'Bold' },
  { label: 'icon-italic', value: 'ITALIC', title: 'Italic' },
  { label: 'icon-underline', value: 'UNDERLINE', title: 'Underline' },
];

const controlButtons = [
  { label: 'icon-markdown', value: 'markdown', title: 'Markdown' },
  { label: 'icon-html', value: 'html', title: 'HTML' },
];

function stateFromPlainText(text: string, type: ?string): ContentState {
  switch (type) {
    case 'html':
      return stateFromHTML(text);
    case 'markdown':
      return stateFromMarkdown(text);
    default:
      return ContentState.createFromText(text);
  }
}

function stateToPlainText(state: ContentState, type: ?string): string {
  switch (type) {
    case 'html':
      return stateToHTML(state);
    case 'markdown':
      return stateToMarkdown(state);
    default:
      return state.getPlainText('\n');
  }
}

export default class RichTextEditor extends Component {
  props: Props;

  state: State = {
    editorState: this.valueToEditorState(this.props.value),
    contentType: 'html',
    richMode: true,
  };

  setDisplayProps(contentType: string, richMode: boolean): void {
    const contentState: ContentState = this.state.editorState.getCurrentContent();
    let newContent: ContentState = contentState;

    if (this.state.richMode != richMode) {
      if (richMode) {
        newContent = stateFromPlainText(stateToPlainText(contentState), this.state.contentType);
      } else {
        newContent = stateFromPlainText(stateToPlainText(contentState, contentType));
      }
    }

    this.setState({
      contentType,
      richMode,
      editorState: EditorState.moveFocusToEnd(EditorState.createWithContent(newContent)),
    });
  }

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

  get headerButtons(): ?Element<*> {
    const { editorState } = this.state;
    const selection = editorState.getSelection();
    const blockType = editorState
      .getCurrentContent()
      .getBlockForKey(selection.getStartKey())
      .getType();

    return (
      <div
        className={classNames('fc-rich-text-editor__command-set', s.set)}
        key="header-buttons">
        <Dropdown
          className="fc-rich-text-editor__command-headers"
          placeholder={<i className="icon-size" />}
          onChange={this.handleBlockTypeChange}
          value={blockType}
          items={headerStyles.map(t => [t.value, t.label])}
        />
      </div>
    );
  }

  get listButtons(): ?Element<*> {
    const { editorState } = this.state;
    const selection = editorState.getSelection();
    const blockType = editorState
      .getCurrentContent()
      .getBlockForKey(selection.getStartKey())
      .getType();


    const isActive = value => value == blockType;
    return this.renderToggleButtons(listStyles, isActive, this.handleBlockTypeChange, {
      key: 'list-buttons',
    });
  }

  get styleButtons(): ?Element<*> {
    const currentStyle = this.state.editorState.getCurrentInlineStyle();
    const isActive = value => currentStyle.has(value);
    return this.renderToggleButtons(inlineStyles, isActive, this.handleStyleClick, {
      key: 'style-buttons',
    });
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

  get controlButtons(): Element<*> {
    const isActive = value => value == this.state.contentType && !this.state.richMode;

    return this.renderToggleButtons(controlButtons, isActive, contentType => {
      this.setDisplayProps(contentType, !this.state.richMode);
    });
  }

  get markdownLink() {
    return (
      <a
        className="fc-rich-text-editor__link"
        href="http://markdown-guide.readthedocs.io/en/latest/basics.html"
        target="_blank"
      >
        Markdown Cheatsheet
      </a>
    );
  }

  get commandBarContent() {
    if (this.state.richMode) {
      return [
        this.headerButtons,
        this.styleButtons,
        this.listButtons,
      ];
    } else if (this.state.contentType == 'markdown') {
      return this.markdownLink;
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

  get htmlContent(): string {
    const contentState: ContentState = this.state.editorState.getCurrentContent();
    if (this.state.contentType != 'html' && !this.state.richMode) {
      return stateToHTML(stateFromPlainText(stateToPlainText(contentState), this.state.contentType));
    }

    if (this.state.richMode) {
      return stateToHTML(contentState);
    } else {
      return stateToPlainText(contentState);
    }
  }

  @autobind
  handleBlur() {
    this.props.onChange(this.htmlContent);
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

  renderToggleButtons(buttonsData: Array<ButtonData>,
                      isActive: (v: string) => boolean,
                      onClick: (v: any) => void,
                      props: ?Object): Element<*> {
    const buttons = buttonsData.map(type => {
      return (
        <ToggleButton
          className={`fc-rich-text-editor__btn-${type.value}`}
          isActive={isActive(type.value)}
          labelIcon={type.label}
          onClick={onClick}
          value={type.value}
          key={type.label}
          title={type.title}
        />
      );
    });

    return <div className={classNames('fc-rich-text-editor__command-set', s.set)} {...props}>{buttons}</div>;
  }

  shouldComponentUpdate(nextProps: Props, nextState: State): boolean {
    return (
      this.state.editorState != nextState.editorState ||
        this.state.contentType != nextState.contentType ||
        this.state.richMode != nextState.richMode
    );
  }

  render() {
    const { editorState, contentType, richMode } = this.state;

    const className = classNames(
      'fc-rich-text-editor',
      this.props.className,
      `_content-type-${contentType}`, {
        '_rich-mode': richMode,
      }
    );

    return (
      <div className={className}>
        {this.props.label && <div className="fc-rich-text-editor__label">{this.props.label}</div>}
        <div className="fc-rich-text-editor__command-bar">
          <div className="fc-rich-text-editor__main-commands">
            {this.commandBarContent}
          </div>
          <div>
            {this.controlButtons}
          </div>
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
