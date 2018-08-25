if (!String.prototype.format) {
    String.prototype.format = function() {
        var args = arguments;
        return this.replace(/{(\d+)}/g, function(match, number) {
            return typeof args[number] != 'undefined'
                ? args[number]
                : match
                ;
        });
    };
}

var mymap = L.map('map').setView([55.777574, 37.709657], 13);
L.tileLayer('/image/z{z}/{x}.{y}.png', {
    maxZoom: 17,
    minZoom: 10,
}).addTo(mymap);

mymap.on('click', map_click);

const search_box = document.getElementById("search_box");
const search_hint_list = document.getElementById("search_hint_list");

//search_box.addEventListener('focus', function (event) {
//    search_hint_list.style.display = "block";
//});
search_box.addEventListener('click', function (event) {
    search_hint_list.style.display = "block";
    event.stopPropagation();
});
document.body.addEventListener('click', function (ev) {
    search_hint_list.style.display = "none";
});
//search_box.addEventListener('blur', function (event) {
//    search_hint_list.style.display = "none";
//});

const price_range = document.getElementById("price_range");
const target_price = document.getElementById("target_price");

var current_selection = {id: -1};
var current_poly = {remove: function (){}};

var road_layer = {remove: function (){}, exist: false};
var price_shown = false;
var price_layer;
var search_hint_request = undefined;

const tools_container = document.getElementById("tools_container");
const side_container = document.getElementById("side_container");
const apartment_list = document.getElementById("apartment_list");
const infrastructure_list = document.getElementById("infrastructure_list");
const current_address = document.getElementById("current_address");

var is_foot_mode = true;
var radio_foot_true = document.getElementById("radio_foot_true");
var radio_foot_false = document.getElementById("radio_foot_false");

const property_tab_button = document.getElementById("property_tab_button");
const infrastructure_tab_button = document.getElementById("infrastructure_tab_button");
infrastructure_tab_button.addEventListener('click', switch_tabs);
var is_property_tab = true;

const lists_container = document.getElementById("lists_container");
window.onresize = function (event) {
    console.log("resized");
    lists_container.style.height = (side_container.offsetHeight - tools_container.offsetHeight) + "px"
};

function map_click(event) {
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function(){
        if (this.readyState === 4 && this.status === 200) {
            console.log(this.responseText);
            var ans = JSON.parse(this.responseText);
            current_poly.remove();
            road_layer.remove();

            if (ans.status === "success") {
                if (ans.objects[0].id !== current_selection.id){
                    current_poly.remove();
                    current_poly = L.polygon(ans.objects[0].points).addTo(mymap);
                    delete ans.objects[0].points;
                    current_selection = ans.objects[0];

                    road_layer = L.tileLayer('/api/tile/road?z={z}&x={x}&y={y}&start_id=' + current_selection.id + '&max_dist=18000&foot=' + is_foot_mode + '&absolute=true',
                        {
                            maxZoom: 17,
                            minZoom: 10,
                        }).addTo(mymap);
                    if (price_shown === true){
                        price_shown = false;
                        price_layer.remove();
                    }
                    console.log(current_selection.data["addr:street"]);
                    console.log(current_selection.data["addr:housenumber"]);
                    current_address.innerText = (current_selection.data["addr:street"] + " " + current_selection.data["addr:housenumber"]);

                    let aparts = document.getElementsByClassName("apartment");
                    while (aparts.length !== 0){
                        aparts[0].remove();
                    }

                    for (let i=0; i<current_selection.data.apartments.length; i++){
                        let d = document.createElement("div");
                        d.classList.add("apartment");
                        let table = document.createElement("table");
                        table.style.width = "100%";
                        let ap = current_selection.data.apartments[i]["full data"];
                        let keys = Object.keys(ap);
                        let a = document.createElement("a");
                        a.innerText = ap["url"];
                        a.href = ap["url"];
                        d.appendChild(a);
                        for (let j=0; j<keys.length; j++){
                            if(keys[j] !== "url" && keys[j] !== "coords" && keys[j] !== "Адрес" && keys[j] !== "Общая информация") {
                                table.innerHTML += '<tr><td class="table_heading_name">{0}</td><td>{1}</td></tr>'.format(keys[j], ap[keys[j]]);
                            }
                        }
                        d.appendChild(table);
                        apartment_list.appendChild(d);
                    }
                    show_infrastructure();
                }else{
                    current_selection = {id: -1};

                    let aparts = document.getElementsByClassName("apartment");
                    while (aparts.length !== 0){
                        aparts[0].remove();
                    }
                    current_address.innerText = "";
                }
            }else {
                current_selection = {id: -1};

                let aparts = document.getElementsByClassName("apartment");
                while (aparts.length !== 0){
                    aparts[0].remove();
                }
                current_address.innerText = "";
            }
        }
    };
    xhttp.open("GET", '/api/search/point?lat=' + event.latlng.lat + "&lon=" + event.latlng.lng);
    xhttp.send();
    console.log("clicked at map!");
    console.log("lat: " + event.latlng.lat + " lon: " + event.latlng.lng);
}

function show_infrastructure() {
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            let ans = JSON.parse(xhttp.responseText);
            let tb = document.createElement("table");
            tb.innerHTML = "<colgroup><col width='80%'><col width='20%'></colgroup>";
            tb.style.width = "100%";
            for (let i=0; i<ans.objects.length; i++){
                if (ans.objects[i].data.name !== undefined) {
                    tb.innerHTML += "<tr><td>{0}</td><td>{1}</td></tr>".format(ans.objects[i].data.name, Math.round(ans.objects[i].dist / 600) + " мин.");
                }
            }
            infrastructure_list.innerHTML = "";
            infrastructure_list.appendChild(tb);
            console.log("dsakldlas");
        }
    };
    xhttp.open("GET", '/api/search/close_objects?id=' + current_selection.id + '&max_dist=6000&foot=' + is_foot_mode + '&max_num=150');
    xhttp.send();
}

function show_prices() {
    var pr = Number(target_price.value);
    var pr_rn = Number(price_range.value);

    if (price_shown){
        price_layer.remove();
        price_shown = false;
    }else{
        price_layer = L.tileLayer('/api/tile/price?z={z}&x={x}&y={y}&price=' + pr + '&range=' + pr_rn + '&absolute=true',
            {
                maxZoom: 17,
                minZoom: 10,
            }).addTo(mymap);
        price_shown = true;
    }

}

function search_hints() {
    if (search_hint_list.style.display === "none"){
        search_hint_list.style.display = "block";
    }
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function(){
        if (this.readyState === 4 && this.status === 200) {
            let ans = JSON.parse(xhttp.responseText);
            if (ans.status === "success"){
                search_hint_list.innerHTML = "";
                for (let i=0; i<ans.suggestions.length; i++){
                    let b = document.createElement("button");
                    b.classList.add("suggest_hint");
                    b.innerText = ans.suggestions[i];
                    b.addEventListener('click', search_hint_click);
                    search_hint_list.appendChild(b);
                }
            }
        }
    };
    xhttp.open("GET", '/api/search/predict?text=' + search_box.value + "&suggestions=10");
    xhttp.send();
}

function foot_mode_update() {
    if ((is_foot_mode && radio_foot_false.checked) || (!is_foot_mode && radio_foot_true.checked)){
        is_foot_mode = !is_foot_mode;
        if (road_layer.exist !== false){
            road_layer.remove();
            road_layer = L.tileLayer('/api/tile/road?z={z}&x={x}&y={y}&start_id=' + current_selection.id + '&max_dist=18000&foot=' + is_foot_mode + '&absolute=true',
                {
                    maxZoom: 17,
                    minZoom: 10,
                }).addTo(mymap);
        }
    }
}

function search_box_search() {
    let xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function () {
        if (this.readyState === 4 && this.status === 200) {
            let ans = JSON.parse(xhttp.responseText);
            if (ans.status === "found"){
                let max_lat = -366, min_lat = 366, max_lon = -366, min_lon = 366;
                for (let i=0; i<ans.result.points.length; i++){
                    let p = ans.result.points[i];
                    max_lat = Math.max(max_lat, p[0]); max_lon = Math.max(max_lon, p[1]);
                    min_lat = Math.min(min_lat, p[0]); min_lon = Math.min(min_lon, p[1]);
                }
                mymap.fitBounds([
                    [min_lat, max_lon],
                    [max_lat, max_lon]
                ]);
            }else if(ans.status === "not found"){

            }
        }
    };
    xhttp.open("GET", '/api/search/string?text=' + search_box.value + "&latlon=true");
    xhttp.send();
}

function search_hint_click(event) {
    search_box.value = event.target.innerText;
    search_box_search();
}

function switch_tabs(event) {
    if (is_property_tab){
        is_property_tab = false;
        property_tab_button.addEventListener('click', switch_tabs);
        infrastructure_tab_button.removeEventListener('click', switch_tabs);
        property_tab_button.classList.replace("w3-white", "w3-pale-green");
        infrastructure_tab_button.classList.replace("w3-pale-green", "w3-white");
        apartment_list.style.display = "none";
        infrastructure_list.style.display = "block";
    }else{
        is_property_tab = true;
        property_tab_button.removeEventListener('click', switch_tabs);
        infrastructure_tab_button.addEventListener('click', switch_tabs);
        property_tab_button.classList.replace("w3-pale-green", "w3-white");
        infrastructure_tab_button.classList.replace("w3-white", "w3-pale-green");
        apartment_list.style.display = "block";
        infrastructure_list.style.display = "none";
    }
}