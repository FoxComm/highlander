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

type Props = {
  label?: string,
  onChange: (content: string) => void,
  value: ?string,
};

type State = { editorState: Object };

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

  get styleButtons(): Array<Element> {
    const currentStyle = this.state.editorState.getCurrentInlineStyle();
    return inlineStyles.map(type => {
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
  }

  @autobind
  handleStyleClick(style: string, event: Object) {
    stopPropagation(event);
    this.handleChange(RichUtils.toggleInlineStyle(this.state.editorState, style));
  }

  @autobind
  handleChange(editorState: Object) {
    const contentState: ContentState = editorState.getCurrentContent();
    const rawHTML: string = stateToHTML(contentState);

    this.setState({
      editorState,
    }, () => this.props.onChange(rawHTML));
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
          {this.styleButtons}        
        </div>
        <div className="fc-rich-text-editor__content">
          <Editor editorState={editorState}
                  handleKeyCommand={this.handleKeyCommand}
                  onChange={this.handleChange} />
        </div>
      </div>
    );
  }
}
