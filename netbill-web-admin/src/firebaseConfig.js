import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";
import { getAuth } from "firebase/auth";

// Your shared Firebase configuration
const firebaseConfig = {
    apiKey: "AIzaSyAbVMsdooGwLAqwPMCdbsTvvEHVIV1qOWE",
    authDomain: "netbill-isp.firebaseapp.com",
    databaseURL: "https://netbill-isp-default-rtdb.asia-southeast1.firebasedatabase.app",
    projectId: "netbill-isp",
    storageBucket: "netbill-isp.firebasestorage.app",
    messagingSenderId: "208674105924",
    appId: "1:208674105924:web:8891b343538b3f7eb02a11"
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);
