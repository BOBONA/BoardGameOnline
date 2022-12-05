document.addEventListener('DOMContentLoaded', function() {
    var elems = document.querySelectorAll('.modal');
    var instances = M.Modal.init(elems);
    if ((new URL(window.location.href)).searchParams.get("confirm") === "") {
        instances.filter(modal => modal.id === "confirmEmail")[0].open();
    }
});