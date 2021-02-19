# Tugas Besar 1 Startegi Algoritma - Kelompok 43

## Algoritma Greddy
Algoritma greedy yang kami terapkan pada bot game ini memiliki tujuan untuk mengumpulkan score sebanyak (maksimal) mungkin dan memberikan damage sebanyak mungkin kepada musuh.
Fungsi seleksi dari algoritma yang diterapkan akan mengecek keadaaan worm sesuai dengan skala prioritas yang telah ditetapkan. Pertama akan dicek apakah worm dengan id 2 dapat melakukan banana bomb. Jika tidak, kemudian akan dicek worm id 3 apakah bisa melakukan snowball.
Jika tidak, akan dicek apakah currentworm dapat melakukan shooting. Jika tidak, akan dicek adakah worm lain yang dapat melakukan shooting. Jika tidak, akan dicek apakah currentworm dapat melakukan digging. Koordinat tempat yang akan di digging akan dipilih koordinant yang jaraknya paling dekat dengan musuh.
Jika tidak ada ada yang bisa di digging, baru worm moving yang koordinatnya dipilih koordinat yang arahnya mendekati musuh.

## Requirement Program
1. Java (minimal Java 8): https://www.oracle.com/java/technologies/javase/javasejdk8-downloads.html
2. IntelIJ IDEA: https://www.jetbrains.com/idea/
3. NodeJS: https://nodejs.org/en/download/

## Cara Penggunaan Program
Untuk dapat menjalankan bot permainan ini, Anda hanya perlu membuka file “run.bat” yang terdapat pada folder src (Untuk
Windows/Mac dapat dibuka dengan double-click, Untuk Linux dapat menjalankan command “make run”).

## Author
1. 13519034 Ruhiyah Faradishi Widiaputri (K01)
2. 13519097 Nabila Hannania (K02)
3. 13519165 Kadek Surya Mahardika (K03)
