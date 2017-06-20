
// libs
import _ from 'lodash';
import classNames from 'classnames';
import React from 'react';
import PropTypes from 'prop-types';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { createSelector } from 'reselect';
import { bindActionCreators } from 'redux';

// components
import ConfirmationModal from 'components/core/confirmation-modal';
import { PrimaryButton } from 'components/core/button';
import SectionTitle from '../section-title/section-title';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import NoteForm from './form';
import LiveSearchAdapter from '../live-search/live-search-adapter';
import NoteRow from './note-row';

// redux
import * as notesActions from '../../modules/notes';

const editingNote = createSelector(
  state => _.get(state.notes.list.currentSearch(), 'results.rows', []),
  state => _.get(state.notes, 'editingNoteId'),
  (notes, editingNoteId) => {
    return _.find(notes, {id: editingNoteId});
  }
);

function mapStateToProps(state) {
  return {
    ...state.notes,
    editingNote: editingNote(state),
  };
}

function mapDispatchToProps(dispatch, props) {
  return {
    ...bindActionCreators(notesActions, dispatch),
    searchActions: bindActionCreators(notesActions.actions, dispatch),
  };
}

/*eslint "react/prop-types": 0*/

@connect(mapStateToProps, mapDispatchToProps)
export default class Notes extends React.Component {
  static deleteOptions = {
    title: 'Confirm',
    label: 'Are you sure you want to delete this note?',
    confirmLabel: 'Yes',
    cancelLabel: 'No'
  };

  static propTypes = {
    entity: PropTypes.shape({
      entityId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
      entityType: PropTypes.string.isRequired
    })
  };

  componentDidMount() {
    this.props.setCurrentEntity(this.props.entity);
    this.props.searchActions.resetSearch();
    this.props.searchActions.fetch();
  }

  get isCustomerNotes() {
    return this.props.entity.entityType == 'customer';
  }

  get tableColumns() {
    let baseColumns = [
      {field: 'body', text: 'Note'},
      {field: 'author', text: 'Author'}
    ];
    if (this.isCustomerNotes) {
      baseColumns = [
        {field: 'transaction', text: 'Transaction'},
        ...baseColumns,
      ];
    }

    return [
      {field: 'createdAt', text: 'Date/Time', type: 'datetime'},
      ...baseColumns
    ];
  }

  @autobind
  renderNoteRow(row, index, isNew) {
    if (this.props.editingNoteId === row.id) {
      return (
        <TableRow key={`row-${row.id}`}>
          <TableCell colSpan={this.tableColumns.length}>
            <NoteForm
              body={this.props.editingNote && this.props.editingNote.body}
              onReset={this.props.stopAddingOrEditingNote}
              onSubmit={data => this.props.editNote(this.props.editingNoteId, data)}
            />
          </TableCell>
        </TableRow>
      );
    } else {
      return (
        <NoteRow
          note={row}
          columns={this.tableColumns}
          params={isNew}
          actions={this.props}
          key={`row-${row.id}`}
          />
      );
    }
  }

  @autobind
  injectAddingForm(rows, columns) {
    // -1 means we want add new note
    if (this.props.editingNoteId === -1) {
      return [
        <TableRow key="row-add">
          <TableCell colSpan={columns.length}>
            <NoteForm
              onReset={this.props.stopAddingOrEditingNote}
              onSubmit={this.props.createNote}
            />
          </TableCell>
        </TableRow>,
        ...rows
      ];
    }
    return rows;
  }

  get controls() {
    return (
      <PrimaryButton icon="add" onClick={this.props.startAddingNote} disabled={!!this.props.isAddingNote } />
    );
  }

  get sectionClassName() {
    const entityType = this.props.entity.entityType;
    return `fc-${entityType}-notes`;
  }

  render() {
    const props = this.props;
    const cls = classNames('fc-notes', this.sectionClassName);

    return (
      <div className={cls} >
        <SectionTitle className="fc-grid-gutter fc-notes-section-title" title="Notes">{this.controls}</SectionTitle>
        <LiveSearchAdapter
          searches={props.list}
          searchActions={props.searchActions}
          singleSearch={true}
          placeholder="keyword search"
          >
          <TableView
            emptyMessage="No notes found."
            data={props.list.currentSearch().results}
            renderRow={this.renderNoteRow}
            columns={this.tableColumns}
            processRows={this.injectAddingForm}
          />
        </LiveSearchAdapter>
        <ConfirmationModal
          {...Notes.deleteOptions}
          isVisible={this.props.noteIdToDelete != null}
          onCancel={() => this.props.stopDeletingNote(this.props.noteIdToDelete)}
          onConfirm={() => this.props.deleteNote(this.props.noteIdToDelete)}
        />
      </div>
    );
  }
}
