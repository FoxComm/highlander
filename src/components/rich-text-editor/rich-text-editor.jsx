/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { stateFromHTML } from 'draft-js-import-html';
import { stateToHTML } from 'draft-js-export-html';
import { autobind } from 'core-decorators';

// components
import { ContentState, Editor, EditorState, RichUtils } from 'draft-js';

type Props = { onChange: (content: string) => void, value: ?string };
type State = { editorState: Object };

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
        <Editor editorState={editorState}
                handleKeyCommand={this.handleKeyCommand}
                onChange={this.handleChange} />
      </div>
    );
  }
}
