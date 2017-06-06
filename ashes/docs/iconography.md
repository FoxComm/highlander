```
const container = { 
  display: 'flex',
  flexWrap: 'wrap',
  justifyContent: 'center',
};

const box =  {
  display: 'flex',
  flexDirection: 'column',
  margin: '10px',
  color: '#6d859e'
}

const iconWrapper =  {
  width: '120px',
  height: '120px',
  border: '1px dashed #bdc9d6',
  textAlign: 'center',
  lineHeight: '120px',
  fontSize: '28px'
  
}

const text = {
  marginTop: '5px',
  fontSize: '12px',
  textAlign: 'center',
  lineHeight: '15px'
}

const names = [
  "add",
  "bell",
  "chevron-right",
  "chevron-left",
  "chevron-up",
  "chevron-down",
  "customer",
  "customers",
  "discounts",
  "drag-drop",
  "edit",
  "external-link-2",
  "external-link",
  "gift-cards",
  "help",
  "home",
  "inventory",
  "items",
  "orders",
  "returns",
  "settings-col",
  "settings",
  "success",
  "error",
  "trash",
  "warning",
  "calendar",
  "lock",
  "unlock",
  "usd",
  "search",
  "visa",
  "dinners",
  "store-credit",
  "amex",
  "discover",
  "jcb",
  "close",
  "warning-dark",
  "success-dark",
  "error-dark",
  "check",
  "list",
  "grid",
  "barcode",
  "category",
  "back",
  "filter",
  "share",
  "ellipsis",
  "minus",
  "location",
  "down",
  "up",
  "desktop",
  "tablet",
  "sort",
  "mastercard",
  "google",
  "phone",
  "mobile",
  "upload",
  "save",
  "save-2",
  "heart",
  "promotion",
  "numbers",
  "align-left",
  "align-right",
  "bullets",
  "align-center",
  "align-justify",
  "indent_increase",
  "indent_decrease",
  "hyperlink",
  "size",
  "bold",
  "underline",
  "italic",
  "html",
  "dot",
  "category-collapse",
  "category-expand",
  "hierarchy",
  "hierarchy-rotated",
  "clear-formatting",
  "markdown",
  "export",
];

const Iconography = () => {
    const icons = names.map((name) => (
    <div key={name} style={box}>
      <div style={iconWrapper}> 
        <Icon name={name}/>
      </div>
      <span style={text}>{name}</span>
    </div>
  ));

  return(
    <div style={container}>
      {icons}
    </div>
  );
};

<Iconography />
```
