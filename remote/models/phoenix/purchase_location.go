package phoenix

const (
	// PurchaseOnFox means that the channel's transactions happen on the platform.
	PurchaseOnFox = iota

	// PurchaseOffFox means the channel's transactions happen off the platform.
	PurchaseOffFox
)

// PurchaseLocation signifies whether a channel's transactions occur on the
// platform, or whether they occur in a different location.
type PurchaseLocation int64
