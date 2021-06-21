## Steps for Developers 
1. Download the latest stable release of [Android Studio](https://developer.android.com/studio/index.html).
2. Fork the [Project Repository](https://github.com/openfoodfacts/openfoodfacts-androidapp) by clicking "Fork".
3. Clone the repository <br>`git clone https://github.com/YOUR_USERNAME/openfoodfacts-androidapp.git`.
4. Change directory `cd openfoodfacts-androidapp`.
5. Run the project in Android Studio


## Remote Configuration
After you clone the repository the default remote is `origin` and it refers to your fork on Github. You must keep track of the changes made to the original repository by setting up another remote named `upstream`.

1. Open terminal in your repository and type <br>`git remote add origin https://github.com/YOUR_USERNAME/openfoodfacts-androidapp.git`
2. Type <br>` git remote add upstream https://github.com/openfoodfacts/openfoodfacts-androidapp `<br>to add upstream.
3. Type ` git remote -v ` and you should see <br>
```
   origin  https://github.com/YOUR_USERNAME/openfoodfacts-androidapp.git (fetch)
   origin  https://github.com/YOUR_USERNAME/openfoodfacts-androidapp.git (push) 
   upstream        https://github.com/openfoodfacts/openfoodfacts-androidapp.git (fetch)
   upstream        https://github.com/openfoodfacts/openfoodfacts-androidapp.git (push)
```

To now update your local copy type <br> `git fetch upstream` <br> `git merge upstream/master` <br> `git push`

## Steps for creating a Pull Request

1. Checkout to the master branch `git checkout master`
2. Sync `git pull`
3. Start a new branch with a suitable name `git checkout -b branch_name`
4. Develop a new feature or solve a existing issue 
5. Add the changed files `git add file_name`
6. Commit with a suitable message `git commit -m " Changes made "`
7. Push `git push origin branch_name`
8. Go to the Github Repositoy and create a pull request according to the [PULL_REQUEST_TEMPLATE](https://github.com/openfoodfacts/openfoodfacts-androidapp/blob/master/.github/PULL_REQUEST_TEMPLATE.md).
