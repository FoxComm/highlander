//libs
import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

//data
import operators from '../../../paragons/customer-groups/operators';
import { actions as groupActions } from '../../../modules/customer-groups/dynamic/group';
import { actions as listActions } from '../../../modules/customer-groups/dynamic/list';

//helpers
import { prefix } from '../../../lib/text-utils';


const prefixed = prefix('fc-customer-group-dynamic');

const mapStateToProps = state => ({list: state.customerGroups.dynamic.list});
const mapDispatchToProps = dispatch => ({
  groupActions: bindActionCreators(groupActions, dispatch),
  listActions: bindActionCreators(listActions, dispatch),
});

@connect(mapStateToProps, mapDispatchToProps)
export default class DynamicGroup extends Component {

  static propTypes = {
    list: PropTypes.object,
    group: PropTypes.shape({
      id: PropTypes.number,
      name: PropTypes.string,
      mainCondition: PropTypes.oneOf([
        operators.and,
        operators.or,
      ]),
      conditions: PropTypes.arrayOf(PropTypes.array),
    }),
  };

  componentDidMount() {
    this.props.listActions.fetch();
    setTimeout(()=> {
      this.props.groupActions.setFilterTerm('very');
      this.props.listActions.fetch();
    }, 1000);
  }

  render() {
    const {list, group} = this.props;

    return (
      <div className={prefixed('')}>
        <div className="fc-grid">
          <header className="fc-col-md-1-1">
            <h1 className="fc-title">
              {group.name}
            </h1>
          </header>
          <article className="fc-col-md-1-1">
            <div>
              Dynamic Group `{group.name}` details
            </div>
          </article>
        </div>
      </div>
    );
  }
}
