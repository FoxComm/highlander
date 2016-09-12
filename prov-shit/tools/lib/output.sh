section() {
    local hr=printf "$1%.s" $(seq 1 $(tput cols))
    echo $hr
    echo $2
    echo $hr
}
