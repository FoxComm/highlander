/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';

// components
import { Editor, EditorState, RichUtils } from 'draft-js';

type Props = {};
type State = { editorState: Object };

export default class RichTextEditor extends Component<void, Props, State> {
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { editorState: EditorState.createEmpty() };
  }

  @autobind
  handleChange(editorState: Object) {
    this.setState({ editorState });
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