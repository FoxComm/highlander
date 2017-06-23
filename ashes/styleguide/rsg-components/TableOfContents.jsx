import { pick } from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { filterSectionsByName } from 'react-styleguidist/lib/utils/utils';
import ComponentsList from 'rsg-components/ComponentsList';
import TableOfContentsRenderer from 'react-styleguidist/lib/rsg-components/TableOfContents/TableOfContentsRenderer';

function isElementWithSlugInViewport(slug) {
  const el = document.getElementById(slug);
  if (!el) {
    console.warn(`element "${slug}" not found`);

    return false;
  }
  const elementTop = el.offsetTop;
  const elementBottom = elementTop + el.offsetHeight;
  const pageTop = window.scrollY;

  return elementTop + 50 <= pageTop + 200 && elementBottom >= pageTop;
}

function updateHash(hash) {
  if (history.pushState) {
    history.pushState(null, null, `#/${hash}`);
  } else {
    location.hash = `#/${hash}`;
  }
}

export default class TableOfContents extends Component {
  static propTypes = {
    sections: PropTypes.array.isRequired,
  };

  state = {
    sections: [],
    searchTerm: '',
    components: [],
    expandedItems: {},
  };

  componentWillMount() {
    const buildItems = level => (res, section) => {
      const children = [...(section.sections || []), ...(section.components || [])];

      const item = {
        ...pick(section, ['sections', 'components', 'name', 'slug']),
        heading: !!section.name && children.length > 0,
        items: children.length > 0 && children.reduce(buildItems(level + 1), []),
        isLeaf: !children.length,
        collapsible: level > 0 && children.length > 0,
        level,
      };

      return [...res, item];
    };

    const sections = this.props.sections.reduce(buildItems(0), []);

    console.log(sections);

    this.setState({ sections });
  }

  componentDidMount() {
    window.addEventListener('scroll', this.handleScroll);
  }

  getExpandedState(slug, expand = true) {
    return {
      ...this.state.expandedItems,
      [slug]: expand,
    };
  }

  @autobind
  handleScroll() {
    const _reduce = parent => (res, item) => {
      if (res) return res;

      if (item.isLeaf) {
        if (isElementWithSlugInViewport(item.slug)) {
          return { parent, element: item };
        } else {
          return null;
        }
      } else {
        return item.items.reduce(_reduce(item), res);
      }
    };

    const res = this.state.sections.reduce(_reduce(null), null);

    if (!res) {
      return;
    }

    if (this.activeItem !== res.element.slug) {
      updateHash(res.element.slug);

      this.setState({
        expandedItems: this.getExpandedState(parent.slug, true),
      });
    } else {
      this.setState({
        expandedItems: this.getExpandedState(parent.slug, false),
      });
    }
  }

  @autobind
  handleClick(slug, e) {
    e.preventDefault();

    document.querySelector(e.target.hash).scrollIntoView({
      behavior: 'smooth',
    });

    const itemExpanded = this.state.expandedItems[slug];

    this.setState({ expandedItems: this.getExpandedState(slug, !itemExpanded) });
  }

  get activeItem() {
    return window.location.hash.substr(2);
  }

  renderLevel(sections, level = 0) {
    const items = sections.map(section => {
      return {
        ...pick(section, ['heading', 'isLeaf', 'name', 'slug', 'collapsible']),
        content: section.items.length > 0 && this.renderLevel(section.items, level + 1),
        expanded: this.state.expandedItems[section.slug],
      };
    });

    const hasLeaves = items.some(({ content }) => !content);

    return <ComponentsList items={items} level={level} isLeaves={hasLeaves} onItemClick={this.handleClick} />;
  }

  renderSections() {
    const { searchTerm } = this.state;
    const { sections } = this.state;

    // If there is only one section, we treat it as a root section
    // In this case the name of the section won't be rendered and it won't get left padding
    const filtered = filterSectionsByName(sections, searchTerm);

    return this.renderLevel(filtered, 0);
  }

  render() {
    const { searchTerm } = this.state;
    return (
      <TableOfContentsRenderer searchTerm={searchTerm} onSearchTermChange={searchTerm => this.setState({ searchTerm })}>
        {this.renderSections()}
      </TableOfContentsRenderer>
    );
  }
}
